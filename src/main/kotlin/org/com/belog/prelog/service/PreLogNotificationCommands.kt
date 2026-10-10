package org.com.belog.prelog.service

import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.command.CreateNotificationCommand

internal object PreLogNotificationCommands {
    private const val PLAN_CREATED_MESSAGE_SUFFIX = " 님이 새로운 계획을 등록했어요"
    private const val PLAN_LIKED_MESSAGE_SUFFIX = " 님이 좋아요를 눌렀어요"

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

    fun planLiked(
        meetingId: Long,
        planId: Long,
        likerGroupMemberId: Long,
        recipientUserId: Long,
        likerUserId: Long,
        likerNickname: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = likerUserId,
            type = NotificationType.PRE_LOG_PLAN_LIKED,
            message = likerNickname + PLAN_LIKED_MESSAGE_SUFFIX,
            targetId = meetingId,
            deduplicationKey = "${NotificationType.PRE_LOG_PLAN_LIKED}:$planId:$likerGroupMemberId",
        )
}
