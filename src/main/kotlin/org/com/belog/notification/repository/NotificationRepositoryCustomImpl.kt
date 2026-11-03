package org.com.belog.notification.repository

import jakarta.persistence.EntityManager
import org.com.belog.notification.domain.Notification
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

open class NotificationRepositoryCustomImpl(
    private val entityManager: EntityManager,
) : NotificationRepositoryCustom {
    @Transactional(propagation = Propagation.MANDATORY)
    override fun saveIfAbsent(notification: Notification) {
        entityManager
            .createNativeQuery(SAVE_IF_ABSENT_SQL)
            .setParameter("recipientUserId", requireNotNull(notification.recipient.id))
            .setParameter("actorUserId", notification.actor?.id)
            .setParameter("type", notification.type.name)
            .setParameter("message", notification.message)
            .setParameter("targetId", notification.targetId)
            .setParameter("deduplicationKey", notification.deduplicationKey)
            .executeUpdate()
    }

    companion object {
        private const val SAVE_IF_ABSENT_SQL =
            """
            INSERT INTO notifications (
                recipient_user_id,
                actor_user_id,
                type,
                message,
                target_id,
                deduplication_key,
                read_at,
                created_at,
                updated_at
            ) VALUES (
                :recipientUserId,
                :actorUserId,
                :type,
                :message,
                :targetId,
                :deduplicationKey,
                NULL,
                CURRENT_TIMESTAMP(6),
                CURRENT_TIMESTAMP(6)
            )
            ON DUPLICATE KEY UPDATE id = id
            """
    }
}
