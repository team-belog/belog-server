package org.com.belog.notification.service.command

import org.com.belog.notification.domain.NotificationType

data class CreateNotificationCommand(
    val recipientUserId: Long,
    val actorUserId: Long?,
    val type: NotificationType,
    val message: String,
    val targetId: Long?,
    val deduplicationKey: String,
)
