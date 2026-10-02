package org.com.belog.notification.service.result

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
    val targetId: Long?,
    val read: Boolean,
    val createdAt: Instant,
)
