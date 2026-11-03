package org.com.belog.notification.domain

enum class NotificationOutboxStatus {
    PENDING,
    PROCESSING,
    SENT,
    SKIPPED,
    FAILED,
}
