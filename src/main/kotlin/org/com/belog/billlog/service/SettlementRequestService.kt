package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
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
    fun complete(
        meetingId: Long,
        settlementRequestId: Long,
        requesterUserId: Long,
    ) {
        val settlementRequest =
            settlementRequestRepository.findByIdAndMeetingIdForUpdate(settlementRequestId, meetingId)
                ?: throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_NOT_FOUND)

        if (settlementRequest.participant.groupMember.user.id != requesterUserId) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_ACCESS_DENIED)
        }

        settlementRequest.complete(Instant.now(clock))
    }
}
