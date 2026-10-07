package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
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
        settlementRequestId: Long,
        requesterUserId: Long,
    ) {
        val settlementRequest =
            settlementRequestRepository.findByIdForUpdate(settlementRequestId)
                ?: throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_NOT_FOUND)

        if (settlementRequest.participant.groupMember.user.id != requesterUserId) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_ACCESS_DENIED)
        }

        settlementRequest.complete(Instant.now(clock))
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun validateMeetingSettled(meetingId: Long) {
        val pendingRequestId =
            settlementRequestRepository.findFirstIdByMeetingIdAndStatusForUpdate(
                meetingId = meetingId,
                status = SettlementRequestStatus.PENDING.name,
            )
        if (pendingRequestId != null) {
            throw BusinessException(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS)
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    fun validateGroupSettled(groupId: Long) {
        val pendingRequestId =
            settlementRequestRepository.findFirstIdByGroupIdAndStatusForUpdate(
                groupId = groupId,
                status = SettlementRequestStatus.PENDING.name,
            )
        if (pendingRequestId != null) {
            throw BusinessException(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS)
        }
    }
}
