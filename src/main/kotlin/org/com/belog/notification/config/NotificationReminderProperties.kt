package org.com.belog.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "notification.reminder")
data class NotificationReminderProperties(
    val batchSize: Int,
) {
    init {
        require(batchSize > 0) { "리마인드 알림 배치 크기는 0보다 커야 합니다." }
    }
}
