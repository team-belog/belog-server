package org.com.belog.notification.repository

import org.com.belog.notification.domain.NotificationOutbox
import java.time.Instant

interface NotificationOutboxRepositoryCustom {
    fun saveIfAbsentByDeduplicationKey(deduplicationKey: String)

    fun claimBatch(
        now: Instant,
        limit: Int,
    ): List<NotificationOutbox>
}
