package org.com.belog.billlog.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object BillLogNotificationCommands {
    private const val SETTLEMENT_REQUESTED_MESSAGE_SUFFIX = " 님이 정산을 요청했어요"
    private const val BILL_REGISTERED_MESSAGE_SUFFIX = " 님이 영수증을 등록했어요"

    fun settlementRequested(
        meetingId: Long,
        settlementRequestId: Long,
        recipientUserId: Long,
        payerUserId: Long,
        payerNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = payerUserId,
            type = NotificationType.SETTLEMENT_REQUESTED,
            message = payerNickname + SETTLEMENT_REQUESTED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.SETTLEMENT_REQUESTED}:$settlementRequestId",
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
}
