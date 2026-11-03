package org.com.belog.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "notification.outbox")
data class NotificationOutboxWorkerProperties(
    val batchSize: Int,
    val maxAttempts: Int,
    val processingTimeout: Duration,
    val retryBaseDelay: Duration,
) {
    init {
        require(batchSize > 0) { "Outbox 배치 크기는 0보다 커야 합니다." }
        require(maxAttempts > 0) { "Outbox 최대 시도 횟수는 0보다 커야 합니다." }
        require(processingTimeout.isPositive) { "Outbox 처리 제한 시간은 0보다 커야 합니다." }
        require(retryBaseDelay.isPositive) { "Outbox 재시도 기본 지연 시간은 0보다 커야 합니다." }
    }
}
