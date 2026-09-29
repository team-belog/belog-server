package org.com.belog.billlog.service

import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.result.BillLogSummaryResult
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.repository.MeetingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BillLogService(
    private val meetingRepository: MeetingRepository,
    private val groupMemberRepository: GroupMemberRepository,
    private val billRepository: BillRepository,
    private val settlementRequestRepository: SettlementRequestRepository,
) {
    @Transactional(readOnly = true)
    fun getSummary(
        meetingId: Long,
        userId: Long,
    ): BillLogSummaryResult {
        val meeting =
            meetingRepository.findByIdWithGroup(meetingId)
                ?: throw BusinessException(MeetingErrorCode.MEETING_NOT_FOUND)
        val groupId = checkNotNull(meeting.group.id) { "Bill-log 대상 만남의 그룹 ID가 없습니다." }

        if (!groupMemberRepository.existsByGroupIdAndUserId(groupId, userId)) {
            throw BusinessException(GroupErrorCode.NOT_GROUP_MEMBER)
        }

        return BillLogSummaryResult(
            totalSpentAmount = billRepository.sumTotalAmountByMeetingId(meetingId),
            completedParticipantCount =
                settlementRequestRepository.countCompletedParticipantsByMeetingId(
                    meetingId = meetingId,
                    pendingStatus = SettlementRequestStatus.PENDING,
                ),
            pendingParticipantCount =
                settlementRequestRepository.countDistinctParticipantsByMeetingIdAndStatus(
                    meetingId = meetingId,
                    status = SettlementRequestStatus.PENDING,
                ),
        )
    }
}
