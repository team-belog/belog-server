package org.com.belog.notification.worker

import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.com.belog.notification.domain.NotificationOutboxStatus
import org.com.belog.notification.service.NotificationOutboxProcessingService
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Component
@Profile("!test")
class NotificationOutboxMetricsRecorder(
    private val processingService: NotificationOutboxProcessingService,
    meterRegistry: MeterRegistry,
) {
    private val outboxCounts: Map<NotificationOutboxStatus, AtomicLong> =
        MONITORED_STATUSES.associateWith { status ->
            AtomicLong().also { count ->
                Gauge
                    .builder(OUTBOX_MESSAGES_METRIC, count) { it.toDouble() }
                    .description("상태별 알림 Outbox 건수")
                    .tag(STATUS_TAG, status.name.lowercase())
                    .register(meterRegistry)
            }
        }

    @Scheduled(fixedDelay = REFRESH_INTERVAL_SECONDS, timeUnit = TimeUnit.SECONDS)
    fun refresh() {
        processingService
            .countByStatuses(MONITORED_STATUSES)
            .forEach { (status, count) -> outboxCounts.getValue(status).set(count) }
    }

    companion object {
        private const val OUTBOX_MESSAGES_METRIC = "notification.outbox.messages"
        private const val STATUS_TAG = "status"
        private const val REFRESH_INTERVAL_SECONDS = 60L
        private val MONITORED_STATUSES =
            setOf(
                NotificationOutboxStatus.PENDING,
                NotificationOutboxStatus.PROCESSING,
                NotificationOutboxStatus.FAILED,
            )
    }
}
