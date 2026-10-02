package org.com.belog.notification.service

import org.com.belog.notification.domain.Notification
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.notification.service.result.NotificationListItemResult
import org.com.belog.notification.service.result.NotificationListResult
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class NotificationQueryService(
    private val notificationRepository: NotificationRepository,
) {
    @Transactional(readOnly = true)
    fun hasUnreadNotification(userId: Long): Boolean = notificationRepository.existsByRecipientIdAndReadAtIsNull(userId)

    @Transactional(readOnly = true)
    fun getNotifications(
        userId: Long,
        cursor: Long?,
        size: Int = DEFAULT_PAGE_SIZE,
    ): NotificationListResult {
        require(cursor == null || cursor > 0) { "알림 목록 커서는 양수여야 합니다." }
        require(size in MIN_PAGE_SIZE..MAX_PAGE_SIZE) {
            "알림 목록 조회 개수는 ${MIN_PAGE_SIZE}개 이상 ${MAX_PAGE_SIZE}개 이하여야 합니다."
        }

        val notifications =
            notificationRepository.findPageByRecipientUserId(
                recipientUserId = userId,
                cursor = cursor,
                pageable = PageRequest.of(0, size + NEXT_PAGE_LOOKAHEAD_COUNT),
            )
        val hasNext = notifications.size > size
        val pageItems = notifications.take(size)

        return NotificationListResult(
            items = pageItems.map { notification -> notification.toListItemResult() },
            nextCursor = pageItems.lastOrNull()?.id?.takeIf { hasNext },
            hasNext = hasNext,
        )
    }

    private fun Notification.toListItemResult(): NotificationListItemResult =
        NotificationListItemResult(
            notificationId = requireNotNull(id),
            type = type,
            message = message,
            targetId = targetId,
            read = isRead,
            createdAt = requireNotNull(createdAt),
        )

    companion object {
        const val DEFAULT_PAGE_SIZE = 20
        const val MIN_PAGE_SIZE = 1
        const val MAX_PAGE_SIZE = 50
        private const val NEXT_PAGE_LOOKAHEAD_COUNT = 1
    }
}
