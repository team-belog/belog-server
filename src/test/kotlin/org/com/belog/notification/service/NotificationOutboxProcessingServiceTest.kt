package org.com.belog.notification.service

import io.micrometer.core.instrument.MeterRegistry
import org.com.belog.notification.domain.Notification
import org.com.belog.notification.domain.NotificationDevice
import org.com.belog.notification.domain.NotificationOutbox
import org.com.belog.notification.domain.NotificationOutboxStatus
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.infrastructure.PushSendOutcome
import org.com.belog.notification.infrastructure.PushSendResult
import org.com.belog.notification.repository.NotificationDeviceRepository
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationOutboxProcessingServiceTest {
    @Autowired
    private lateinit var processingService: NotificationOutboxProcessingService

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationDeviceRepository: NotificationDeviceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var meterRegistry: MeterRegistry

    @MockitoBean
    private lateinit var clock: Clock

    @AfterEach
    fun cleanUp() {
        notificationOutboxRepository.deleteAllInBatch()
        notificationDeviceRepository.deleteAllInBatch()
        notificationRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `일부 기기 발송에 성공하면 재시도 없이 발송 성공으로 기록된다`() {
        val now = Instant.parse("2026-10-09T00:00:00Z")
        `when`(clock.instant()).thenReturn(now)
        val recipient = saveUser("recipient")
        val outbox = createProcessingOutbox(recipient, "send-success-key")
        val results =
            listOf(
                PushSendResult("token-success", PushSendOutcome.SUCCESS),
                PushSendResult("token-retryable", PushSendOutcome.RETRYABLE_FAILURE),
            )

        processingService.applyResults(requireNotNull(outbox.id), results)

        val updated = notificationOutboxRepository.findById(requireNotNull(outbox.id)).orElseThrow()
        assertEquals(NotificationOutboxStatus.SENT, updated.status)
        assertEquals(now, updated.completedAt)
    }

    @Test
    fun `전부 일시적 오류면 재시도 대상으로 남는다`() {
        val now = Instant.parse("2026-10-09T00:00:00Z")
        `when`(clock.instant()).thenReturn(now)
        val recipient = saveUser("recipient")
        val outbox = createProcessingOutbox(recipient, "retry-key")
        val results = listOf(PushSendResult("token-retryable", PushSendOutcome.RETRYABLE_FAILURE))

        processingService.applyResults(requireNotNull(outbox.id), results)

        val updated = notificationOutboxRepository.findById(requireNotNull(outbox.id)).orElseThrow()
        assertEquals(NotificationOutboxStatus.PENDING, updated.status)
        assertEquals(now.plus(Duration.ofSeconds(30)), updated.nextAttemptAt)
        assertTrue(requireNotNull(updated.lastFailureReason).contains("RETRYABLE_FAILURE"))
    }

    @Test
    fun `재시도로 해결되지 않는 오류만 있으면 즉시 최종 실패로 기록되고 무효 토큰 기기는 비활성화된다`() {
        val now = Instant.parse("2026-10-09T00:00:00Z")
        `when`(clock.instant()).thenReturn(now)
        val recipient = saveUser("recipient")
        val device = registerDevice(recipient, "device-1", "token-invalid")
        val outbox = createProcessingOutbox(recipient, "final-failure-key")
        val results =
            listOf(
                PushSendResult("token-invalid", PushSendOutcome.INVALID_TOKEN),
                PushSendResult("token-permanent", PushSendOutcome.PERMANENT_FAILURE),
            )

        processingService.applyResults(requireNotNull(outbox.id), results)

        val updated = notificationOutboxRepository.findById(requireNotNull(outbox.id)).orElseThrow()
        assertEquals(NotificationOutboxStatus.FAILED, updated.status)
        assertEquals(now, updated.completedAt)
        val updatedDevice = notificationDeviceRepository.findById(requireNotNull(device.id)).orElseThrow()
        assertFalse(updatedDevice.isActive)
    }

    @Test
    fun `발송 결과를 반영하면 토큰별 발송 결과가 결과 유형별로 집계된다`() {
        `when`(clock.instant()).thenReturn(Instant.parse("2026-10-09T00:00:00Z"))
        val recipient = saveUser("recipient")
        val outbox = createProcessingOutbox(recipient, "push-metric-key")
        val successBefore = pushSendCount("success")
        val invalidTokenBefore = pushSendCount("invalid_token")
        val results =
            listOf(
                PushSendResult("token-success", PushSendOutcome.SUCCESS),
                PushSendResult("token-invalid-1", PushSendOutcome.INVALID_TOKEN),
                PushSendResult("token-invalid-2", PushSendOutcome.INVALID_TOKEN),
            )

        processingService.applyResults(requireNotNull(outbox.id), results)

        assertEquals(successBefore + 1, pushSendCount("success"))
        assertEquals(invalidTokenBefore + 2, pushSendCount("invalid_token"))
    }

    @Test
    fun `상태별 Outbox 건수를 조회하면 해당 상태가 없는 경우 0으로 반환된다`() {
        `when`(clock.instant()).thenReturn(Instant.parse("2026-10-09T00:00:00Z"))
        val recipient = saveUser("recipient")
        createProcessingOutbox(recipient, "processing-key")

        val counts =
            processingService.countByStatuses(
                setOf(NotificationOutboxStatus.PROCESSING, NotificationOutboxStatus.FAILED),
            )

        assertEquals(
            mapOf(NotificationOutboxStatus.PROCESSING to 1L, NotificationOutboxStatus.FAILED to 0L),
            counts,
        )
    }

    private fun pushSendCount(outcome: String): Double =
        meterRegistry
            .get("notification.push.sends")
            .tag("outcome", outcome)
            .counter()
            .count()

    private fun saveUser(providerUserId: String): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ),
        )

    private fun registerDevice(
        user: User,
        deviceId: String,
        fcmToken: String,
    ): NotificationDevice = notificationDeviceRepository.saveAndFlush(NotificationDevice.register(user, deviceId, fcmToken))

    private fun createProcessingOutbox(
        recipient: User,
        deduplicationKey: String,
    ): NotificationOutbox {
        val notification =
            notificationRepository.saveAndFlush(
                Notification.create(
                    recipient = recipient,
                    actor = null,
                    type = NotificationType.PRE_LOG_PLAN_CREATED,
                    message = "발송 결과 테스트 알림",
                    targetId = 1L,
                    deduplicationKey = deduplicationKey,
                ),
            )
        val outbox = notificationOutboxRepository.saveAndFlush(NotificationOutbox.create(notification, deduplicationKey))
        outbox.markProcessing(Instant.now(clock))
        return notificationOutboxRepository.saveAndFlush(outbox)
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
