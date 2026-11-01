package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.MeetingMemberOptionResult
import org.com.belog.meeting.service.result.MeetingMemberResult
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeetingMemberService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberFinder: GroupMemberFinder,
    private val groupMemberRepository: GroupMemberRepository,
    private val meetingParticipantRepository: MeetingParticipantRepository,
    private val userService: UserService,
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
        validateOwner(meeting, userId)

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

    private fun validateOwner(
        meeting: Meeting,
        userId: Long,
    ) {
        if (meeting.owner.user.id != userId) {
            throw BusinessException(MeetingErrorCode.NOT_MEETING_OWNER)
        }
    }
}
