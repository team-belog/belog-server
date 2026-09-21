package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingAvailableDate
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.domain.MeetingScheduleResponse
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingAvailableDateRepository
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
import org.com.belog.meeting.service.result.CandidateDatePollResult
import org.com.belog.meeting.service.result.CandidateDateRangeResult
import org.com.belog.meeting.service.result.DatePollMemberResult
import org.com.belog.meeting.service.result.MeetingDatePollResult
import org.com.belog.meeting.service.result.MeetingDatePollResults
import org.com.belog.meeting.service.result.MyDatePollResponseResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class MeetingDatePollService(
    private val meetingRepository: MeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository,
    private val meetingScheduleResponseRepository: MeetingScheduleResponseRepository,
    private val meetingAvailableDateRepository: MeetingAvailableDateRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getDatePoll(
        meetingId: Long,
        userId: Long,
    ): MeetingDatePollResult {
        val participant = findParticipant(meetingId, userId)
        val meeting = participant.meeting
        validateDatePollMeeting(meeting)

        val candidateDateRanges = meetingCandidateDateRangeRepository.findAllByMeetingIdOrderByDate(meetingId)

        return MeetingDatePollResult(
            meetingId = requireNotNull(meeting.id),
            status = meeting.status,
            candidateDateRanges =
                candidateDateRanges.map { candidate ->
                    CandidateDateRangeResult(
                        id = requireNotNull(candidate.id),
                        startDate = candidate.startDate,
                        endDate = candidate.endDate,
                    )
                },
        )
    }

    @Transactional(readOnly = true)
    fun getDatePollResults(
        meetingId: Long,
        userId: Long,
    ): MeetingDatePollResults {
        val participants = meetingParticipantRepository.findAllWithMemberAndUserByMeetingId(meetingId)
        if (participants.none { participant -> participant.groupMember.user.id == userId }) {
            throwParticipantLookupException(meetingId)
        }

        val meeting = participants.first().meeting
        validateDatePollMeeting(meeting)

        val candidates = meetingCandidateDateRangeRepository.findAllByMeetingIdOrderByDate(meetingId)
        val responses = meetingScheduleResponseRepository.findAllWithParticipantByMeetingId(meetingId)
        val availableDates = meetingAvailableDateRepository.findAllWithResponseParticipantAndCandidateByMeetingId(meetingId)

        val creatorParticipant =
            participants.first { participant -> meeting.isCreatedBy(participant.groupMember) }
        val creatorParticipantId = requireNotNull(creatorParticipant.id)
        val respondedParticipantIds =
            responses.mapTo(mutableSetOf()) { response -> requireNotNull(response.participant.id) }
        val selectedParticipantIdsByCandidateId =
            availableDates.groupBy(
                keySelector = { availableDate -> requireNotNull(availableDate.candidateDateRange.id) },
                valueTransform = { availableDate -> requireNotNull(availableDate.response.participant.id) },
            )

        val memberByParticipantId =
            participants.associate { participant ->
                requireNotNull(participant.id) to participant.toMemberResult()
            }

        val candidateResults =
            candidates
                .map { candidate ->
                    val selectedParticipantIds = selectedParticipantIdsByCandidateId[requireNotNull(candidate.id)].orEmpty().toSet()
                    val availableParticipantIds = selectedParticipantIds + creatorParticipantId
                    val unavailableParticipantIds = respondedParticipantIds - selectedParticipantIds

                    CandidateDatePollResult(
                        candidateDateRangeId = requireNotNull(candidate.id),
                        startDate = candidate.startDate,
                        endDate = candidate.endDate,
                        rank = 0,
                        availableCount = availableParticipantIds.size,
                        availableMembers =
                            participants
                                .mapNotNull { participant ->
                                    memberByParticipantId[requireNotNull(participant.id)]
                                        ?.takeIf { requireNotNull(participant.id) in availableParticipantIds }
                                },
                        unavailableMembers =
                            participants
                                .mapNotNull { participant ->
                                    memberByParticipantId[requireNotNull(participant.id)]
                                        ?.takeIf { requireNotNull(participant.id) in unavailableParticipantIds }
                                },
                    )
                }.sortedByDescending(CandidateDatePollResult::availableCount)
                .mapIndexed { index, result -> result.copy(rank = index + 1) }

        return MeetingDatePollResults(
            meetingId = requireNotNull(meeting.id),
            totalParticipantCount = participants.size,
            respondedParticipantCount = respondedParticipantIds.size + 1,
            candidateDateResults = candidateResults,
        )
    }

    @Transactional
    fun respondDatePoll(
        meetingId: Long,
        userId: Long,
        candidateDateRangeIds: List<Long>,
    ) {
        val participant = findParticipantForUpdate(meetingId, userId)
        val meeting = participant.meeting
        validateDatePollMeeting(meeting)
        if (meeting.isCreatedBy(participant.groupMember)) {
            throw BusinessException(MeetingErrorCode.CREATOR_CANNOT_RESPOND_DATE_POLL)
        }
        if (meeting.status != MeetingStatus.SCHEDULING) {
            throw BusinessException(MeetingErrorCode.DATE_POLL_NOT_SCHEDULING)
        }

        val participantId = requireNotNull(participant.id)
        if (meetingScheduleResponseRepository.findByMeetingIdAndParticipantIdForUpdate(meetingId, participantId) != null) {
            throw BusinessException(MeetingErrorCode.DATE_POLL_ALREADY_RESPONDED)
        }
        if (candidateDateRangeIds.distinct().size != candidateDateRangeIds.size) {
            throw BusinessException(MeetingErrorCode.DUPLICATE_AVAILABLE_DATE)
        }

        val selectedCandidates =
            if (candidateDateRangeIds.isEmpty()) {
                emptyList()
            } else {
                meetingCandidateDateRangeRepository.findAllByMeetingIdAndIdIn(
                    meetingId = meetingId,
                    candidateDateRangeIds = candidateDateRangeIds,
                )
            }
        if (selectedCandidates.size != candidateDateRangeIds.size) {
            throw BusinessException(MeetingErrorCode.INVALID_AVAILABLE_DATE)
        }

        val response =
            meetingScheduleResponseRepository.save(
                MeetingScheduleResponse.create(
                    meeting = meeting,
                    participant = participant,
                    respondedAt = Instant.now(clock),
                ),
            )
        meetingAvailableDateRepository.saveAll(
            selectedCandidates.map { candidate -> MeetingAvailableDate.create(response, candidate) },
        )
    }

    private fun findParticipant(
        meetingId: Long,
        userId: Long,
    ): MeetingParticipant =
        meetingParticipantRepository.findByMeetingIdAndGroupMemberUserId(meetingId, userId)
            ?: throwParticipantLookupException(meetingId)

    private fun findParticipantForUpdate(
        meetingId: Long,
        userId: Long,
    ): MeetingParticipant =
        meetingParticipantRepository.findByMeetingIdAndUserIdForUpdate(meetingId, userId)
            ?: throwParticipantLookupException(meetingId)

    private fun throwParticipantLookupException(meetingId: Long): Nothing {
        if (!meetingRepository.existsById(meetingId)) {
            throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        }
        throw BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT)
    }

    private fun validateDatePollMeeting(meeting: Meeting) {
        if (meeting.scheduleType != MeetingScheduleType.POLL) {
            throw BusinessException(MeetingErrorCode.NOT_DATE_POLL_MEETING)
        }
    }

    private fun findMyResponse(
        meeting: Meeting,
        participant: MeetingParticipant,
        allCandidateIds: List<Long>,
    ): MyDatePollResponseResult {
        if (meeting.isCreatedBy(participant.groupMember)) {
            return MyDatePollResponseResult(
                responded = true,
                respondedAt = null,
                selectedCandidateDateRangeIds = allCandidateIds,
            )
        }

        val response =
            meetingScheduleResponseRepository.findByMeetingIdAndParticipantId(
                meetingId = requireNotNull(meeting.id),
                participantId = requireNotNull(participant.id),
            ) ?: return MyDatePollResponseResult(
                responded = false,
                respondedAt = null,
                selectedCandidateDateRangeIds = emptyList(),
            )
        val selectedCandidateIds =
            meetingAvailableDateRepository
                .findAllWithCandidateByResponseId(requireNotNull(response.id))
                .map { availableDate -> requireNotNull(availableDate.candidateDateRange.id) }

        return MyDatePollResponseResult(
            responded = true,
            respondedAt = response.respondedAt,
            selectedCandidateDateRangeIds = selectedCandidateIds,
        )
    }

    private fun MeetingParticipant.toMemberResult(): DatePollMemberResult =
        DatePollMemberResult(
            groupMemberId = requireNotNull(groupMember.id),
            nickname = requireNotNull(groupMember.user.nickname),
        )
}
