package org.com.belog.notification.repository

import jakarta.persistence.LockModeType
import org.com.belog.notification.domain.Notification
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationRepositoryCustom {
    @Query(
        """
        SELECT notification
        FROM Notification notification
        WHERE notification.recipient.id = :recipientUserId
          AND (:cursor IS NULL OR notification.id < :cursor)
        ORDER BY notification.id DESC
        """,
    )
    fun findPageByRecipientUserId(
        @Param("recipientUserId") recipientUserId: Long,
        @Param("cursor") cursor: Long?,
        pageable: Pageable,
    ): List<Notification>

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query(
        """
        SELECT notification
        FROM Notification notification
        WHERE notification.id = :notificationId
          AND notification.recipient.id = :recipientUserId
        """,
    )
    fun findByIdAndRecipientUserIdForUpdate(
        @Param("notificationId") notificationId: Long,
        @Param("recipientUserId") recipientUserId: Long,
    ): Notification?
}
