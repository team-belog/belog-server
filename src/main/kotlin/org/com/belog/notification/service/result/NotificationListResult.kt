package org.com.belog.notification.service.result

import org.com.belog.notification.domain.NotificationTargetType
import org.com.belog.notification.domain.NotificationType
import java.time.Instant

data class NotificationListResult(
    val items: List<NotificationListItemResult>,
    val nextCursor: Long?,
    val hasNext: Boolean,
)

data class NotificationListItemResult(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val target: NotificationTargetResult,
    val read: Boolean,
    val createdAt: Instant,
)

data class NotificationTargetResult(
    val type: NotificationTargetType,
    val id: Long?,
)
