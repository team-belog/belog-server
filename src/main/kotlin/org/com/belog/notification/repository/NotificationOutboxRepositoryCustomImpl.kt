package org.com.belog.notification.repository

import jakarta.persistence.EntityManager
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

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
    }
}
