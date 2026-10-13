package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.MeetingMemberResult
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeetingMemberService(
    private val meetingRepository: MeetingRepository,
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
        val groupId = checkNotNull(meeting.group.id) { "조회된 만남의 그룹 ID가 없습니다." }
        if (!groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

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
}
