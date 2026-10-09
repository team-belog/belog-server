package org.com.belog.notification.repository

import org.com.belog.notification.domain.NotificationOutbox
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface NotificationOutboxRepository :
    JpaRepository<NotificationOutbox, Long>,
    NotificationOutboxRepositoryCustom {
    @Modifying
    @Query(
        """
        UPDATE NotificationOutbox outbox
        SET outbox.status = org.com.belog.notification.domain.NotificationOutboxStatus.PENDING
        WHERE outbox.status = org.com.belog.notification.domain.NotificationOutboxStatus.PROCESSING
          AND outbox.processingStartedAt < :staleBefore
        """,
    )
    fun reclaimStaleProcessing(
        @Param("staleBefore") staleBefore: Instant,
    ): Int
}
