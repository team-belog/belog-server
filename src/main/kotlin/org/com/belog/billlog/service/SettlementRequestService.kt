package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.notification.service.NotificationService
import org.com.belog.user.service.UserService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Instant

@Service
class SettlementRequestService(
    private val settlementRequestRepository: SettlementRequestRepository,
    private val notificationService: NotificationService,
    private val userService: UserService,
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
        if (settlementRequest.status == SettlementRequestStatus.COMPLETED) {
            return
        }

        settlementRequest.complete(Instant.now(clock))

        val bill = settlementRequest.bill
        notificationService.create(
            BillLogNotificationCommands.settlementCompleted(
                meetingId = checkNotNull(bill.meeting.id) { "정산 요청 대상 만남의 ID가 없습니다." },
                settlementRequestId = settlementRequestId,
                recipientUserId = checkNotNull(bill.payer.groupMember.user.id) { "결제자의 사용자 ID가 없습니다." },
                debtorUserId = requesterUserId,
            ),
        )
    }

    @Transactional
    fun remind(
        settlementRequestId: Long,
        requesterUserId: Long,
    ) {
        val settlementRequest =
            settlementRequestRepository.findByIdForUpdate(settlementRequestId)
                ?: throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_NOT_FOUND)
        val bill = settlementRequest.bill
        val payerUser = bill.payer.groupMember.user

        if (payerUser.id != requesterUserId) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REMINDER_ACCESS_DENIED)
        }
        if (settlementRequest.status == SettlementRequestStatus.COMPLETED) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_ALREADY_COMPLETED)
        }

        val remindedAt = Instant.now(clock)
        if (!settlementRequest.isReminderCooldownElapsed(remindedAt)) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REMINDER_TOO_FREQUENT)
        }
        settlementRequest.remind(remindedAt)

        notificationService.create(
            BillLogNotificationCommands.settlementReminder(
                meetingId = checkNotNull(bill.meeting.id) { "정산 요청 대상 만남의 ID가 없습니다." },
                settlementRequestId = settlementRequestId,
                recipientUserId =
                    checkNotNull(settlementRequest.participant.groupMember.user.id) {
                        "정산 대상자의 사용자 ID가 없습니다."
                    },
                payerUserId = requesterUserId,
                payerNickname = userService.resolveDisplayNickname(payerUser),
                remindedAt = remindedAt,
            ),
        )
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

    @Transactional(propagation = Propagation.MANDATORY)
    fun validateUserSettled(userId: Long) {
        val pendingRequestId =
            settlementRequestRepository.findFirstIdByUserIdAndStatusForUpdate(
                userId = userId,
                status = SettlementRequestStatus.PENDING.name,
            )
        if (pendingRequestId != null) {
            throw BusinessException(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS)
        }
    }
}
