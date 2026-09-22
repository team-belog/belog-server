package org.com.belog.prelog.service

import org.com.belog.global.error.BusinessException
import org.com.belog.global.time.currentBusinessDate
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.service.result.PreLogMainResult
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class PreLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val clock: Clock,
) {
    @Transactional(readOnly = true)
    fun getPreLogMain(
        meetingId: Long,
        userId: Long,
    ): PreLogMainResult {
        val meeting =
            meetingRepository.findByIdWithGroupAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Pre-log 대상 만남의 그룹 ID가 없습니다." }
        val groupMember =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        val currentDate = clock.currentBusinessDate()

        return PreLogMainResult(
            meetingId = checkNotNull(meeting.id) { "조회된 만남의 ID가 없습니다." },
            meetingName = meeting.name,
            groupId = groupId,
            groupName = meeting.group.name,
            meetingStatus = meeting.status,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            isEnded = meeting.isEnded(currentDate),
            canEditMeeting = meeting.isCreatedBy(groupMember),
        )
    }
}
