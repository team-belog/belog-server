package org.com.belog.notification.service.result

import org.com.belog.notification.domain.NotificationTargetType
import org.com.belog.notification.domain.NotificationType

data class OutboxSendContext(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val targetType: NotificationTargetType,
    val targetId: Long?,
    val pushNotificationEnabled: Boolean,
    val recipientActive: Boolean,
    val activeFcmTokens: List<String>,
) {
    val isSendable: Boolean
        get() = pushNotificationEnabled && recipientActive && activeFcmTokens.isNotEmpty()
}
