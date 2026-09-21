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
import org.com.belog.meeting.service.result.CandidateDateRangeResult
import org.com.belog.meeting.service.result.MeetingDatePollResult
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
}
