package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class SettlementRequestService(
    private val settlementRequestRepository: SettlementRequestRepository,
    private val clock: Clock,
) {
    @Transactional
    fun updateStatus(
        meetingId: Long,
        settlementRequestId: Long,
        payerUserId: Long,
        status: SettlementRequestStatus,
    ) {
        val settlementRequest =
            settlementRequestRepository.findByIdAndMeetingIdForUpdate(settlementRequestId, meetingId)
                ?: throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_NOT_FOUND)

        if (settlementRequest.bill.payer.groupMember.user.id != payerUserId) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_ACCESS_DENIED)
        }
        if (status != SettlementRequestStatus.COMPLETED) {
            throw BusinessException(BillLogErrorCode.INVALID_SETTLEMENT_REQUEST_STATUS)
        }

        settlementRequest.complete(Instant.now(clock))
    }
}
