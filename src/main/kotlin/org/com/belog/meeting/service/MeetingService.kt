package org.com.belog.meeting.service

import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
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
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.command.MeetingNotificationCommands
import org.com.belog.meeting.service.result.CreatedMeeting
import org.com.belog.notification.service.NotificationService
import org.springframework.data.domain.PageRequest
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
    private val settlementRequestService: SettlementRequestService,
    private val notificationService: NotificationService,
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
        val currentDate = clock.currentBusinessDate()

        if (startDate.isBefore(currentDate)) {
            throw BusinessException(MeetingErrorCode.PAST_MEETING_DATE)
        }
        val dateRange = createDateRange(startDate, endDate)

        val meeting =
            meetingRepository.save(
                Meeting.createFixed(
                    group = creator.group,
                    creator = creator,
                    name = name,
                    location = location,
                    dateRange = dateRange,
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
        val currentDate = clock.currentBusinessDate()
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
                    dateRange = dateRange,
                    currentDate = currentDate,
                )
            },
        )
        val meetingId = requireNotNull(meeting.id)
        notificationService.createAll(
            participants.map { participant ->
                MeetingNotificationCommands.datePollStarted(
                    meetingId = meetingId,
                    recipientUserId = requireNotNull(participant.user.id),
                    creatorUserId = creatorUserId,
                )
            },
        )

        return CreatedMeeting(
            meetingId = meetingId,
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

    @Transactional
    fun updateMeetingDate(
        meetingId: Long,
        userId: Long,
        dateRange: MeetingDateRange,
    ): Boolean {
        val meeting = findMeetingForUpdate(meetingId)
        validateScheduleManager(meeting, userId)
        return updateConfirmedDate(meeting, dateRange)
    }

    @Transactional
    fun updateMeeting(
        meetingId: Long,
        userId: Long,
        name: String,
        location: String?,
        dateRange: MeetingDateRange?,
    ) {
        val meeting = findMeetingForUpdate(meetingId)
        validateScheduleManager(meeting, userId)

        when (meeting.status) {
            MeetingStatus.SCHEDULING -> {
                if (dateRange != null) {
                    throw BusinessException(MeetingErrorCode.MEETING_DATE_NOT_CONFIRMED)
                }
            }

            MeetingStatus.CONFIRMED ->
                updateConfirmedDate(
                    meeting = meeting,
                    dateRange = dateRange ?: throw BusinessException(MeetingErrorCode.MEETING_DATE_REQUIRED),
                )
        }

        meeting.updateDetails(name = name, location = location)
    }

    @Transactional
    fun deleteMeeting(
        meetingId: Long,
        userId: Long,
    ) {
        val meeting = findMeetingForUpdate(meetingId)
        validateScheduleManager(meeting, userId)
        settlementRequestService.validateMeetingSettled(meetingId)
        meeting.delete(Instant.now(clock))
    }

    @Transactional
    fun delegateOwnersForWithdrawal(userId: Long) {
        meetingRepository.findAllActiveByOwnerUserId(userId).forEach { meeting ->
            delegateOwnerOrNoOp(requireNotNull(meeting.id), userId)
        }
    }

    private fun delegateOwnerOrNoOp(
        meetingId: Long,
        departingUserId: Long,
    ) {
        val meeting = meetingRepository.findByIdForUpdate(meetingId) ?: return
        if (meeting.owner.user.id != departingUserId) {
            return
        }

        val successor =
            meetingParticipantRepository
                .findEarliestActiveParticipant(
                    meetingId = meetingId,
                    excludedGroupMemberId = requireNotNull(meeting.owner.id),
                    pageable = PageRequest.of(0, 1),
                ).firstOrNull()
                ?.groupMember

        if (successor == null) {
            settlementRequestService.validateMeetingSettled(meetingId)
            meeting.delete(Instant.now(clock))
            return
        }

        meeting.delegateOwnerTo(successor)
    }

    private fun updateConfirmedDate(
        meeting: Meeting,
        dateRange: MeetingDateRange,
    ): Boolean {
        if (meeting.status != MeetingStatus.CONFIRMED) {
            throw BusinessException(MeetingErrorCode.MEETING_DATE_NOT_CONFIRMED)
        }
        if (meeting.startDate == dateRange.startDate && meeting.endDate == dateRange.endDate) {
            return false
        }

        val currentDate = clock.currentBusinessDate()
        if (meeting.isEnded(currentDate)) {
            throw BusinessException(MeetingErrorCode.MEETING_ALREADY_ENDED)
        }
        if (dateRange.endDate.isBefore(currentDate)) {
            throw BusinessException(MeetingErrorCode.PAST_MEETING_DATE_SELECTION)
        }

        return meeting.changeConfirmedDate(
            dateRange = dateRange,
            currentDate = currentDate,
        )
    }

    private fun findMeetingForUpdate(meetingId: Long): Meeting =
        meetingRepository.findByIdForUpdate(meetingId)
            ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)

    private fun validateScheduleManager(
        meeting: Meeting,
        userId: Long,
    ) {
        if (meeting.owner.user.id != userId) {
            throw BusinessException(MeetingErrorCode.NOT_MEETING_OWNER)
        }
    }

    private fun findCreator(
        groupId: Long,
        creatorUserId: Long,
    ): GroupMember {
        groupRepository.findActiveByIdForShare(groupId)
            ?: throw BusinessException(GroupErrorCode.GROUP_NOT_FOUND)

        return groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, creatorUserId)
            ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
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

        val participants = groupMemberRepository.findAllByGroupIdAndIdInAndWithdrawnAtIsNull(groupId, participantMemberIds)
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
    }

    private fun createDateRange(
        startDate: LocalDate,
        endDate: LocalDate,
    ): MeetingDateRange =
        try {
            MeetingDateRange(startDate, endDate)
        } catch (_: IllegalArgumentException) {
            throw BusinessException(MeetingErrorCode.INVALID_MEETING_DATE_RANGE)
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
