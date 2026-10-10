package org.com.belog.prelog.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object PreLogNotificationCommands {
    private const val PLAN_CREATED_MESSAGE_SUFFIX = " 님이 새로운 계획을 등록했어요"

    fun planCreated(
        meetingId: Long,
        planId: Long,
        recipientUserId: Long,
        creatorUserId: Long,
        creatorNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = creatorUserId,
            type = NotificationType.PRE_LOG_PLAN_CREATED,
            message = creatorNickname + PLAN_CREATED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.PRE_LOG_PLAN_CREATED}:$planId:$recipientUserId",
        )
}
