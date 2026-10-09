package org.com.belog.notification.worker

import org.com.belog.notification.infrastructure.PushNotificationPayload
import org.com.belog.notification.infrastructure.PushNotificationSender
import org.com.belog.notification.service.NotificationOutboxProcessingService
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
@Profile("!test")
class NotificationOutboxWorker(
    private val processingService: NotificationOutboxProcessingService,
    private val pushNotificationSender: PushNotificationSender,
) {
    @Scheduled(fixedDelayString = "\${notification.outbox.polling-interval-ms:10000}")
    fun processPendingOutbox() {
        processingService.reclaimStaleProcessing()
        processingService.claimBatch().forEach { outboxId -> processClaimedOutbox(outboxId) }
    }

    private fun processClaimedOutbox(outboxId: Long) {
        try {
            val context = processingService.loadContext(outboxId) ?: return

            if (!context.isSendable) {
                processingService.markSkipped(outboxId)
                return
            }

            val results =
                pushNotificationSender.send(
                    fcmTokens = context.activeFcmTokens,
                    payload =
                        PushNotificationPayload(
                            notificationId = context.notificationId,
                            type = context.type,
                            message = context.message,
                            targetType = context.targetType,
                            targetId = context.targetId,
                        ),
                )

            processingService.applyResults(outboxId, results)
        } catch (exception: Exception) {
            log.error("Outbox 처리 중 예기치 못한 오류가 발생했습니다. outboxId={}", outboxId, exception)
            processingService.markUnexpectedFailure(outboxId, "예기치 못한 오류: ${exception.javaClass.simpleName}")
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(NotificationOutboxWorker::class.java)
    }
}
