package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.command.BillLogNotificationCommands
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

        if (settlementRequest.sender.groupMember.user.id != requesterUserId) {
            throw BusinessException(BillLogErrorCode.SETTLEMENT_REQUEST_ACCESS_DENIED)
        }
        if (settlementRequest.status == SettlementRequestStatus.COMPLETED) {
            return
        }

        settlementRequest.complete(Instant.now(clock))

        notificationService.create(
            BillLogNotificationCommands.settlementCompleted(
                meetingId = checkNotNull(settlementRequest.meeting.id) { "정산 요청 대상 만남의 ID가 없습니다." },
                settlementRequestId = settlementRequestId,
                recipientUserId =
                    checkNotNull(settlementRequest.receiver.groupMember.user.id) {
                        "정산 수취인의 사용자 ID가 없습니다."
                    },
                senderUserId = requesterUserId,
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
        val receiverUser = settlementRequest.receiver.groupMember.user

        if (receiverUser.id != requesterUserId) {
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
                meetingId = checkNotNull(settlementRequest.meeting.id) { "정산 요청 대상 만남의 ID가 없습니다." },
                settlementRequestId = settlementRequestId,
                recipientUserId =
                    checkNotNull(settlementRequest.sender.groupMember.user.id) {
                        "정산 송금자의 사용자 ID가 없습니다."
                    },
                receiverUserId = requesterUserId,
                receiverNickname = userService.resolveDisplayNickname(receiverUser),
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
