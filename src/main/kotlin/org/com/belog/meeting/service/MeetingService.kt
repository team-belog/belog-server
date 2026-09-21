package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.CreatedMeeting
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

@Service
class MeetingService(
    private val groupRepository: GroupRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingRepository: MeetingRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository,
    private val clock: Clock,
) {
    @Transactional
    fun createFixedMeeting(
        groupId: Long,
        creatorUserId: Long,
        name: String,
        location: String?,
        participantMemberIds: List<Long>,
        startDate: LocalDate,
        endDate: LocalDate,
    ): CreatedMeeting {
        val creator = findCreator(groupId, creatorUserId)
        val participants = findParticipants(groupId, creator, participantMemberIds)
        val now = Instant.now(clock)
        val currentDate = LocalDate.now(clock)

        if (startDate.isBefore(currentDate)) {
            throw BusinessException(MeetingErrorCode.PAST_MEETING_DATE)
        }
        if (endDate.isBefore(startDate)) {
            throw BusinessException(MeetingErrorCode.INVALID_MEETING_DATE_RANGE)
        }

        val meeting =
            meetingRepository.save(
                Meeting.createFixed(
                    group = creator.group,
                    creator = creator,
                    name = name,
                    location = location,
                    startDate = startDate,
                    endDate = endDate,
                    confirmedAt = now,
                    currentDate = currentDate,
                ),
            )

        saveParticipants(meeting, creator, participants)

        return CreatedMeeting(
            meetingId = requireNotNull(meeting.id),
            groupId = groupId,
            name = meeting.name,
            location = meeting.location,
            scheduleType = meeting.scheduleType,
            status = meeting.status,
            startDate = requireNotNull(meeting.startDate),
            endDate = requireNotNull(meeting.endDate),
            confirmedAt = requireNotNull(meeting.confirmedAt),
            participantCount = participants.size + CREATOR_COUNT,
        )
    }

    @Transactional
    fun createPollMeeting(
        groupId: Long,
        creatorUserId: Long,
        name: String,
        location: String?,
        participantMemberIds: List<Long>,
        candidateDateRanges: List<MeetingDateRange>,
    ): CreatedMeeting {
        val creator = findCreator(groupId, creatorUserId)
        val participants = findParticipants(groupId, creator, participantMemberIds)
        val currentDate = LocalDate.now(clock)
        validateCandidateDateRanges(candidateDateRanges, currentDate)

        val meeting =
            meetingRepository.save(
                Meeting.createPoll(
                    group = creator.group,
                    creator = creator,
                    name = name,
                    location = location,
                ),
            )

        saveParticipants(meeting, creator, participants)
        meetingCandidateDateRangeRepository.saveAll(
            candidateDateRanges.map { dateRange ->
                MeetingCandidateDateRange.create(
                    meeting = meeting,
                    startDate = dateRange.startDate,
                    endDate = dateRange.endDate,
                    currentDate = currentDate,
                )
            },
        )

        return CreatedMeeting(
            meetingId = requireNotNull(meeting.id),
            groupId = groupId,
            name = meeting.name,
            location = meeting.location,
            scheduleType = meeting.scheduleType,
            status = meeting.status,
            startDate = null,
            endDate = null,
            confirmedAt = null,
            participantCount = participants.size + CREATOR_COUNT,
        )
    }

    private fun findCreator(
        groupId: Long,
        creatorUserId: Long,
    ): GroupMember {
        groupMemberRepository.findByGroupIdAndUserId(groupId, creatorUserId)?.let { creator ->
            return creator
        }

        if (!groupRepository.existsById(groupId)) {
            throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)
        }
        throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
    }

    private fun findParticipants(
        groupId: Long,
        creator: GroupMember,
        participantMemberIds: List<Long>,
    ): List<GroupMember> {
        if (participantMemberIds.size > Group.MAX_MEMBER_COUNT - CREATOR_COUNT) {
            throw BusinessException(MeetingErrorCode.PARTICIPANT_LIMIT_EXCEEDED)
        }
        if (participantMemberIds.distinct().size != participantMemberIds.size) {
            throw BusinessException(MeetingErrorCode.DUPLICATE_PARTICIPANT)
        }
        if (creator.id in participantMemberIds) {
            throw BusinessException(MeetingErrorCode.CREATOR_INCLUDED_AS_PARTICIPANT)
        }

        val participants = groupMemberRepository.findAllByGroupIdAndIdIn(groupId, participantMemberIds)
        if (participants.size != participantMemberIds.size) {
            throw BusinessException(MeetingErrorCode.INVALID_PARTICIPANT)
        }
        return participants
    }

    private fun validateCandidateDateRanges(
        candidateDateRanges: List<MeetingDateRange>,
        currentDate: LocalDate,
    ) {
        if (candidateDateRanges.size !in MeetingCandidateDateRange.MIN_COUNT..MeetingCandidateDateRange.MAX_COUNT) {
            throw BusinessException(MeetingErrorCode.INVALID_CANDIDATE_DATE_RANGE_COUNT)
        }
        if (candidateDateRanges.distinct().size != candidateDateRanges.size) {
            throw BusinessException(MeetingErrorCode.DUPLICATE_CANDIDATE_DATE_RANGE)
        }
        if (candidateDateRanges.any { it.startDate.isBefore(currentDate) }) {
            throw BusinessException(MeetingErrorCode.PAST_MEETING_DATE)
        }
        if (candidateDateRanges.any { it.endDate.isBefore(it.startDate) }) {
            throw BusinessException(MeetingErrorCode.INVALID_MEETING_DATE_RANGE)
        }
    }

    private fun saveParticipants(
        meeting: Meeting,
        creator: GroupMember,
        participants: List<GroupMember>,
    ) {
        meetingParticipantRepository.saveAll(
            (listOf(creator) + participants).map { groupMember ->
                MeetingParticipant.create(meeting, groupMember)
            },
        )
    }

    companion object {
        private const val CREATOR_COUNT = 1
    }
}
