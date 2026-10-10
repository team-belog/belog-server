package org.com.belog.notification.worker

import org.com.belog.notification.config.NotificationReminderProperties
import org.com.belog.notification.domain.NotificationType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class NotificationReminderRunner(
    private val properties: NotificationReminderProperties,
) {
    fun run(
        reminderType: NotificationType,
        findTargetIds: (cursor: Long?, size: Int) -> List<Long>,
        remind: (targetId: Long) -> Unit,
    ) {
        var cursor: Long? = null
        var remindedCount = 0
        var failedCount = 0

        do {
            val targetIds = findTargetIds(cursor, properties.batchSize)
            targetIds.forEach { targetId ->
                try {
                    remind(targetId)
                    remindedCount++
                } catch (exception: Exception) {
                    failedCount++
                    log.error("리마인드 알림 처리 중 오류가 발생했습니다. reminder={}, targetId={}", reminderType, targetId, exception)
                }
            }
            cursor = targetIds.lastOrNull()
        } while (targetIds.size == properties.batchSize)

        log.info("리마인드 알림 처리를 마쳤습니다. reminder={}, reminded={}, failed={}", reminderType, remindedCount, failedCount)
    }

    companion object {
        private val log = LoggerFactory.getLogger(NotificationReminderRunner::class.java)
    }
}
