package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.service.GroupMemberFinder
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.service.result.PreLogMainResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PreLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberFinder: GroupMemberFinder,
) {
    @Transactional(readOnly = true)
    fun getPreLogMain(
        meetingId: Long,
        userId: Long,
    ): PreLogMainResult {
        val meeting =
            meetingRepository.findByIdWithGroupOwnerAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupMember = groupMemberFinder.findActiveMember(meeting.group, userId)

        return PreLogMainResult(
            meetingId = checkNotNull(meeting.id) { "조회된 만남의 ID가 없습니다." },
            meetingName = meeting.name,
            groupId = checkNotNull(meeting.group.id) { "Pre-log 대상 만남의 그룹 ID가 없습니다." },
            groupName = meeting.group.name,
            meetingStatus = meeting.status,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            isEnded = meeting.isEnded(),
            canEditMeeting = meeting.isOwnedBy(groupMember),
        )
    }
}
