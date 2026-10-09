package org.com.belog.notification.repository

import jakarta.persistence.EntityManager
import org.com.belog.notification.domain.NotificationOutbox
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

open class NotificationOutboxRepositoryCustomImpl(
    private val entityManager: EntityManager,
) : NotificationOutboxRepositoryCustom {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun saveIfAbsentByDeduplicationKey(deduplicationKey: String) {
        entityManager
            .createNativeQuery(SAVE_IF_ABSENT_SQL)
            .setParameter("deduplicationKey", deduplicationKey)
            .executeUpdate()
    }

    @Suppress("UNCHECKED_CAST")
    @Transactional(propagation = Propagation.MANDATORY)
    override fun claimBatch(
        now: Instant,
        limit: Int,
    ): List<NotificationOutbox> =
        entityManager
            .createNativeQuery(CLAIM_BATCH_SQL, NotificationOutbox::class.java)
            .setParameter("now", now)
            .setParameter("limit", limit)
            .resultList as List<NotificationOutbox>

    companion object {
        private const val SAVE_IF_ABSENT_SQL =
            """
            INSERT INTO notification_outbox (
                notification_id,
                event_key,
                status,
                attempt_count,
                created_at,
                updated_at
            )
            SELECT
                notification.id,
                notification.deduplication_key,
                'PENDING',
                0,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            FROM notifications notification
            WHERE notification.deduplication_key = :deduplicationKey
            ON DUPLICATE KEY UPDATE notification_outbox.id = notification_outbox.id
            """

        private const val CLAIM_BATCH_SQL =
            """
            SELECT * FROM notification_outbox
            WHERE status = 'PENDING'
              AND (next_attempt_at IS NULL OR next_attempt_at <= :now)
            ORDER BY id
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """
    }
}
