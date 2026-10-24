package org.com.belog.billlog.service.command

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand
import java.time.Instant

internal object BillLogNotificationCommands {
    private const val SETTLEMENT_REQUESTED_MESSAGE_SUFFIX = " 님이 정산을 요청했어요"
    private const val BILL_REGISTERED_MESSAGE_SUFFIX = " 님이 영수증을 등록했어요"
    private const val SETTLEMENT_COMPLETED_MESSAGE = "정산이 완료됐어요"
    private const val SETTLEMENT_REMINDER_MESSAGE_SUFFIX = " 님이 정산을 다시 요청했어요"

    fun settlementRequested(
        meetingId: Long,
        settlementRequestId: Long,
        billId: Long,
        recipientUserId: Long,
        receiverUserId: Long,
        receiverNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = receiverUserId,
            type = NotificationType.SETTLEMENT_REQUESTED,
            message = receiverNickname + SETTLEMENT_REQUESTED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.SETTLEMENT_REQUESTED}:$settlementRequestId:$billId",
        )

    fun billRegistered(
        meetingId: Long,
        billId: Long,
        recipientUserId: Long,
        creatorUserId: Long,
        creatorNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = creatorUserId,
            type = NotificationType.BILL_REGISTERED,
            message = creatorNickname + BILL_REGISTERED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.BILL_REGISTERED}:$billId:$recipientUserId",
        )

    fun settlementCompleted(
        meetingId: Long,
        settlementRequestId: Long,
        recipientUserId: Long,
        senderUserId: Long,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = senderUserId,
            type = NotificationType.SETTLEMENT_COMPLETED,
            message = SETTLEMENT_COMPLETED_MESSAGE,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.SETTLEMENT_COMPLETED}:$settlementRequestId",
        )

    fun settlementReminder(
        meetingId: Long,
        settlementRequestId: Long,
        recipientUserId: Long,
        receiverUserId: Long,
        receiverNickname: String,
        remindedAt: Instant,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = receiverUserId,
            type = NotificationType.SETTLEMENT_REMINDER,
            message = receiverNickname + SETTLEMENT_REMINDER_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.SETTLEMENT_REMINDER}:$settlementRequestId:${remindedAt.toEpochMilli()}",
        )
}
