package org.com.belog.notification.repository

import org.com.belog.notification.domain.NotificationOutboxStatus

data class NotificationOutboxStatusCount(
    val status: NotificationOutboxStatus,
    val count: Long,
)
