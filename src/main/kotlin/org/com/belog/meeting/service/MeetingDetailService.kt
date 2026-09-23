package org.com.belog.meeting.service

import org.com.belog.billlog.repository.BillRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.MeetingLogStatus
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.MeetingDetailResult
import org.com.belog.prelog.repository.PlanRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class MeetingDetailService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val planRepository: PlanRepository,
    private val billRepository: BillRepository,
) {
    @Transactional(readOnly = true)
    fun getMeetingDetail(
        meetingId: Long,
        userId: Long,
    ): MeetingDetailResult {
        val meeting =
            meetingRepository.findByIdWithGroupAndCreator(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "조회된 만남의 그룹 ID가 없습니다." }
        val groupMember =
            groupMemberRepository.findByGroupIdAndUserId(groupId, userId)
                ?: throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)

        return MeetingDetailResult(
            meetingId = checkNotNull(meeting.id) { "조회된 만남의 ID가 없습니다." },
            groupId = groupId,
            groupName = meeting.group.name,
            meetingName = meeting.name,
            scheduleType = meeting.scheduleType,
            meetingStatus = meeting.status,
            startDate = meeting.startDate,
            endDate = meeting.endDate,
            location = meeting.location,
            canEditMeeting = meeting.isCreatedBy(groupMember),
            preLogStatus = toLogStatus(planRepository.existsByMeetingId(meetingId)),
            billLogStatus = toLogStatus(billRepository.existsByMeetingId(meetingId)),
            postLogStatus = MeetingLogStatus.NOT_STARTED,
        )
    }

    private fun toLogStatus(hasData: Boolean): MeetingLogStatus =
        if (hasData) MeetingLogStatus.IN_PROGRESS else MeetingLogStatus.NOT_STARTED
}
