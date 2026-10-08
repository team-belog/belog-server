package org.com.belog.notification.repository

interface NotificationOutboxRepositoryCustom {
    fun saveIfAbsentByDeduplicationKey(deduplicationKey: String)
}
