package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingAvailableDateRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
import org.com.belog.meeting.service.command.MeetingNotificationCommands
import org.com.belog.meeting.service.result.MeetingMemberOptionResult
import org.com.belog.meeting.service.result.MeetingMemberResult
import org.com.belog.notification.service.NotificationService
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class MeetingMemberService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberFinder: GroupMemberFinder,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val meetingScheduleResponseRepository: MeetingScheduleResponseRepository,
    private val meetingAvailableDateRepository: MeetingAvailableDateRepository,
    private val meetingInviteeFinder: MeetingInviteeFinder,
    private val notificationService: NotificationService,
    private val userService: UserService,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getMeetingMembers(
        meetingId: Long,
        userId: Long,
    ): List<MeetingMemberResult> {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        groupMemberFinder.findActiveMember(meeting.group, userId)

        return meetingParticipantRepository
            .findAllActiveWithUserByMeetingId(meetingId)
            .map { participant ->
                val member = participant.groupMember
                val user = member.user
                MeetingMemberResult(
                    groupMemberId = checkNotNull(member.id) { "조회된 만남 참여자의 그룹 멤버 ID가 없습니다." },
                    nickname = userService.resolveDisplayNickname(user),
                    profileImageUrl = userService.resolveProfileImageUrl(user),
                    isMe = user.id == userId,
                )
            }.sortedWith(compareByDescending<MeetingMemberResult> { it.isMe }.thenBy { it.groupMemberId })
    }

    @Transactional(readOnly = true)
    fun getMeetingMemberOptions(
        meetingId: Long,
        userId: Long,
    ): List<MeetingMemberOptionResult> {
        val meeting =
            meetingRepository.findByIdWithGroupOwnerAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        validateMembersEditable(meeting, userId)

        val groupId = checkNotNull(meeting.group.id) { "조회된 만남의 그룹 ID가 없습니다." }
        val participantMemberIds =
            meetingParticipantRepository
                .findAllActiveWithUserByMeetingId(meetingId)
                .mapTo(mutableSetOf()) { participant -> checkNotNull(participant.groupMember.id) }
        val billLogLinkedMemberIds = meetingParticipantRepository.findAllBillLogLinkedGroupMemberIds(meetingId).toSet()
        val unremovableMemberIds =
            billLogLinkedMemberIds + checkNotNull(meeting.owner.id) + checkNotNull(meeting.createdBy.id)

        return groupMemberRepository
            .findAllWithUserByGroupId(groupId)
            .map { member ->
                val groupMemberId = checkNotNull(member.id) { "조회된 그룹 멤버의 ID가 없습니다." }
                val user = member.user
                val isParticipant = groupMemberId in participantMemberIds
                MeetingMemberOptionResult(
                    groupMemberId = groupMemberId,
                    nickname = userService.resolveDisplayNickname(user),
                    profileImageUrl = userService.resolveProfileImageUrl(user),
                    isMe = user.id == userId,
                    isParticipant = isParticipant,
                    isRemovable = isParticipant && groupMemberId !in unremovableMemberIds,
                )
            }.sortedWith(compareByDescending<MeetingMemberOptionResult> { it.isMe }.thenBy { it.groupMemberId })
    }

    @Transactional
    fun updateMeetingMembers(
        meetingId: Long,
        userId: Long,
        participantMemberIds: List<Long>,
    ) {
        val meeting =
            meetingRepository.findByIdWithGroupForUpdate(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        validateMembersEditable(meeting, userId)
        if (meeting.owner.id in participantMemberIds) {
            throw BusinessException(MeetingErrorCode.OWNER_INCLUDED_AS_PARTICIPANT)
        }

        val groupId = checkNotNull(meeting.group.id) { "조회된 만남의 그룹 ID가 없습니다." }
        val requestedMembers = meetingInviteeFinder.findInvitees(groupId, participantMemberIds)
        val requestedMemberIds = participantMemberIds.toSet()
        meetingParticipantRepository.findAllByMeetingIdForUpdate(meetingId)
        val currentParticipants = meetingParticipantRepository.findAllActiveWithUserByMeetingId(meetingId)
        val currentMemberIds = currentParticipants.mapTo(mutableSetOf()) { participant -> participant.groupMember.id }

        val excludedParticipants =
            currentParticipants.filter { participant ->
                !meeting.isOwnedBy(participant.groupMember) && participant.groupMember.id !in requestedMemberIds
            }
        val invitedMembers = requestedMembers.filter { member -> member.id !in currentMemberIds }

        excludeParticipants(meeting, excludedParticipants)
        meetingParticipantRepository.saveAll(invitedMembers.map { member -> MeetingParticipant.create(meeting, member) })
        notifyMemberChanges(
            meeting = meeting,
            ownerUserId = userId,
            invitedUserIds = invitedMembers.map { member -> checkNotNull(member.user.id) },
            excludedUserIds = excludedParticipants.map { participant -> checkNotNull(participant.groupMember.user.id) },
        )
    }

    private fun excludeParticipants(
        meeting: Meeting,
        participants: List<MeetingParticipant>,
    ) {
        if (participants.isEmpty()) {
            return
        }
        if (participants.any { participant -> meeting.isCreatedBy(participant.groupMember) }) {
            throw BusinessException(MeetingErrorCode.CREATOR_CANNOT_BE_EXCLUDED)
        }

        val billLogLinkedMemberIds =
            meetingParticipantRepository.findAllBillLogLinkedGroupMemberIds(checkNotNull(meeting.id)).toSet()
        if (participants.any { participant -> participant.groupMember.id in billLogLinkedMemberIds }) {
            throw BusinessException(MeetingErrorCode.BILL_LOG_PARTICIPANT_CANNOT_BE_EXCLUDED)
        }

        val participantIds = participants.map { participant -> checkNotNull(participant.id) }
        meetingAvailableDateRepository.deleteAllByResponseParticipantIdIn(participantIds)
        meetingScheduleResponseRepository.deleteAllByParticipantIdIn(participantIds)
        meetingParticipantRepository.deleteAllInBatch(participants)
    }

    private fun notifyMemberChanges(
        meeting: Meeting,
        ownerUserId: Long,
        invitedUserIds: List<Long>,
        excludedUserIds: List<Long>,
    ) {
        val meetingId = checkNotNull(meeting.id)
        val groupId = checkNotNull(meeting.group.id)
        val changedAt = Instant.now(clock)
        val changeId = UUID.randomUUID()

        notificationService.createAll(
            invitedUserIds.map { recipientUserId ->
                MeetingNotificationCommands.meetingInvited(
                    meetingId = meetingId,
                    meetingName = meeting.name,
                    recipientUserId = recipientUserId,
                    ownerUserId = ownerUserId,
                    changedAt = changedAt,
                    changeId = changeId,
                )
            } +
                excludedUserIds.map { recipientUserId ->
                    MeetingNotificationCommands.meetingExcluded(
                        meetingId = meetingId,
                        meetingName = meeting.name,
                        groupId = groupId,
                        recipientUserId = recipientUserId,
                        ownerUserId = ownerUserId,
                        changedAt = changedAt,
                        changeId = changeId,
                    )
                },
        )
    }

    private fun validateMembersEditable(
        meeting: Meeting,
        userId: Long,
    ) {
        if (meeting.owner.user.id != userId) {
            throw BusinessException(MeetingErrorCode.NOT_MEETING_OWNER)
        }
        if (meeting.isEnded()) {
            throw BusinessException(MeetingErrorCode.ENDED_MEETING_MEMBERS_NOT_EDITABLE)
        }
    }
}
