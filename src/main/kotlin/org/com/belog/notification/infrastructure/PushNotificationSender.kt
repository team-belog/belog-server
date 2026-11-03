package org.com.belog.notification.infrastructure

import org.com.belog.notification.domain.NotificationTargetType
import org.com.belog.notification.domain.NotificationType

data class PushNotificationPayload(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val targetType: NotificationTargetType,
    val targetId: Long?,
)

enum class PushSendOutcome {
    SUCCESS,
    INVALID_TOKEN,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE,
}

data class PushSendResult(
    val fcmToken: String,
    val outcome: PushSendOutcome,
)

interface PushNotificationSender {
    fun send(
        fcmTokens: List<String>,
        payload: PushNotificationPayload,
    ): List<PushSendResult>
}
