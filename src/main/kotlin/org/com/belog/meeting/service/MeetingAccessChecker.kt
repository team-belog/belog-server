package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.springframework.stereotype.Component

@Component
class MeetingAccessChecker(
    private val groupMemberFinder: GroupMemberFinder,
    private val meetingParticipantRepository: MeetingParticipantRepository,
) {
    fun requireParticipant(
        meeting: Meeting,
        userId: Long,
    ): GroupMember {
        val meetingId = checkNotNull(meeting.id) { "참여자 확인 대상 만남의 ID가 없습니다." }
        val groupMember = groupMemberFinder.findActiveMember(meeting.group, userId)
        val groupMemberId = checkNotNull(groupMember.id) { "만남 참여자 확인 대상의 그룹 멤버 ID가 없습니다." }

        if (!meetingParticipantRepository.existsByMeetingIdAndGroupMemberId(meetingId, groupMemberId)) {
            throw BusinessException(MeetingErrorCode.NOT_MEETING_PARTICIPANT)
        }
        return groupMember
    }
}
