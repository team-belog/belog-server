package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.springframework.stereotype.Component

@Component
class MeetingInviteeFinder(
    private val groupMemberRepository: GroupMemberRepository,
) {
    fun findInvitees(
        groupId: Long,
        inviteeMemberIds: List<Long>,
    ): List<GroupMember> {
        if (inviteeMemberIds.size > MAX_INVITEE_COUNT) {
            throw BusinessException(MeetingErrorCode.PARTICIPANT_LIMIT_EXCEEDED)
        }
        if (inviteeMemberIds.distinct().size != inviteeMemberIds.size) {
            throw BusinessException(MeetingErrorCode.DUPLICATE_PARTICIPANT)
        }
        val invitees = groupMemberRepository.findAllByGroupIdAndIdInAndWithdrawnAtIsNull(groupId, inviteeMemberIds)
        if (invitees.size != inviteeMemberIds.size) {
            throw BusinessException(MeetingErrorCode.INVALID_PARTICIPANT)
        }
        return invitees
    }

    companion object {
        private const val OWNER_COUNT = 1
        const val MAX_INVITEE_COUNT = Group.MAX_MEMBER_COUNT - OWNER_COUNT
    }
}
