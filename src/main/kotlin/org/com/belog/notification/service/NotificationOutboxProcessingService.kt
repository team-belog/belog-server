package org.com.belog.notification.service

import org.com.belog.notification.config.NotificationOutboxWorkerProperties
import org.com.belog.notification.domain.NotificationOutbox
import org.com.belog.notification.domain.NotificationTargetType
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.infrastructure.PushSendOutcome
import org.com.belog.notification.infrastructure.PushSendResult
import org.com.belog.notification.repository.NotificationDeviceRepository
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

data class OutboxSendContext(
    val notificationId: Long,
    val type: NotificationType,
    val message: String,
    val targetType: NotificationTargetType,
    val targetId: Long?,
    val pushNotificationEnabled: Boolean,
    val recipientActive: Boolean,
    val activeFcmTokens: List<String>,
) {
    val isSendable: Boolean
        get() = pushNotificationEnabled && recipientActive && activeFcmTokens.isNotEmpty()
}

@Service
class NotificationOutboxProcessingService(
    private val notificationOutboxRepository: NotificationOutboxRepository,
    private val notificationDeviceRepository: NotificationDeviceRepository,
    private val properties: NotificationOutboxWorkerProperties,
    private val clock: Clock,
) {
    @Transactional
    fun reclaimStaleProcessing() {
        val staleBefore = Instant.now(clock).minus(properties.processingTimeout)
        notificationOutboxRepository.reclaimStaleProcessing(staleBefore)
    }

    @Transactional
    fun claimBatch(): List<Long> {
        val now = Instant.now(clock)
        val claimed = notificationOutboxRepository.claimBatch(now, properties.batchSize)
        claimed.forEach { outbox -> outbox.markProcessing(now) }
        return claimed.mapNotNull { it.id }
    }

    @Transactional(readOnly = true)
    fun loadContext(outboxId: Long): OutboxSendContext? {
        val outbox = notificationOutboxRepository.findById(outboxId).orElse(null) ?: return null
        val notification = outbox.notification
        val recipient = notification.recipient
        val activeFcmTokens =
            notificationDeviceRepository
                .findAllByUserIdAndDeactivatedAtIsNull(requireNotNull(recipient.id))
                .map { device -> device.fcmToken }

        return OutboxSendContext(
            notificationId = requireNotNull(notification.id),
            type = notification.type,
            message = notification.message,
            targetType = notification.type.targetType,
            targetId = notification.targetId,
            pushNotificationEnabled = recipient.pushNotificationEnabled,
            recipientActive = recipient.isActive,
            activeFcmTokens = activeFcmTokens,
        )
    }

    @Transactional
    fun markSkipped(outboxId: Long) {
        val outbox = notificationOutboxRepository.findById(outboxId).orElse(null) ?: return
        outbox.markSkipped(Instant.now(clock))
    }

    @Transactional
    fun applyResults(
        outboxId: Long,
        results: List<PushSendResult>,
    ) {
        val outbox = notificationOutboxRepository.findById(outboxId).orElse(null) ?: return
        val now = Instant.now(clock)

        deactivateInvalidTokenDevices(results, now)

        when {
            results.any { it.outcome == PushSendOutcome.SUCCESS } -> outbox.markSent(now)
            results.any { it.outcome == PushSendOutcome.RETRYABLE_FAILURE } -> applyRetryOrFinalFailure(outbox, results, now)
            else -> outbox.markFinalFailure(now, summarize(results))
        }
    }

    @Transactional
    fun markUnexpectedFailure(
        outboxId: Long,
        reason: String,
    ) {
        val outbox = notificationOutboxRepository.findById(outboxId).orElse(null) ?: return
        val now = Instant.now(clock)

        if (outbox.attemptCount >= properties.maxAttempts) {
            outbox.markFinalFailure(now, reason)
        } else {
            outbox.markFailedForRetry(now.plus(computeBackoff(outbox.attemptCount)), reason)
        }
    }

    private fun applyRetryOrFinalFailure(
        outbox: NotificationOutbox,
        results: List<PushSendResult>,
        now: Instant,
    ) {
        if (outbox.attemptCount >= properties.maxAttempts) {
            outbox.markFinalFailure(now, summarize(results))
            return
        }

        outbox.markFailedForRetry(now.plus(computeBackoff(outbox.attemptCount)), summarize(results))
    }

    private fun deactivateInvalidTokenDevices(
        results: List<PushSendResult>,
        now: Instant,
    ) {
        results
            .filter { it.outcome == PushSendOutcome.INVALID_TOKEN }
            .forEach { result ->
                notificationDeviceRepository
                    .findByFcmTokenAndDeactivatedAtIsNull(result.fcmToken)
                    ?.deactivate(now)
            }
    }

    private fun computeBackoff(attemptCount: Int): Duration {
        val multiplier = 1L shl (attemptCount - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
        return properties.retryBaseDelay.multipliedBy(multiplier)
    }

    private fun summarize(results: List<PushSendResult>): String =
        results
            .groupingBy { it.outcome }
            .eachCount()
            .entries
            .joinToString(", ") { (outcome, count) -> "$outcome:$count" }

    companion object {
        private const val MAX_BACKOFF_SHIFT = 10
    }
}
