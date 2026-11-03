package org.com.belog.notification.service

import org.com.belog.notification.domain.Notification
import org.com.belog.notification.domain.NotificationOutbox
import org.com.belog.notification.domain.NotificationOutboxStatus
import org.com.belog.notification.domain.NotificationType
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
import org.springframework.test.context.TestPropertySource
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = ["notification.outbox.batch-size=5"])
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationOutboxClaimConcurrencyTest {
    @Autowired
    private lateinit var processingService: NotificationOutboxProcessingService

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var clock: Clock

    @AfterEach
    fun cleanUp() {
        notificationOutboxRepository.deleteAllInBatch()
        notificationRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `여러 Worker가 동시에 선점해도 같은 Outbox를 중복으로 가져가지 않는다`() {
        `when`(clock.instant()).thenReturn(Instant.parse("2026-10-09T00:00:00Z"))
        val recipient = saveUser("recipient")
        repeat(OUTBOX_ROW_COUNT) { index -> createPendingOutbox(recipient, "claim-key-$index", index + 1L) }
        val executor = Executors.newFixedThreadPool(CLAIMER_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val claims =
                (1..CLAIMER_COUNT).map {
                    executor.submit<List<Long>> {
                        startSignal.await()
                        processingService.claimBatch()
                    }
                }

            startSignal.countDown()
            val claimedIds = claims.flatMap { claim -> claim.get(10, TimeUnit.SECONDS) }

            assertEquals(OUTBOX_ROW_COUNT, claimedIds.size)
            assertEquals(OUTBOX_ROW_COUNT, claimedIds.toSet().size)
            val processingCount =
                notificationOutboxRepository
                    .findAllById(claimedIds)
                    .count { outbox -> outbox.status == NotificationOutboxStatus.PROCESSING }
            assertEquals(OUTBOX_ROW_COUNT, processingCount)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun saveUser(providerUserId: String): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ),
        )

    private fun createPendingOutbox(
        recipient: User,
        deduplicationKey: String,
        targetId: Long,
    ): NotificationOutbox {
        val notification =
            notificationRepository.saveAndFlush(
                Notification.create(
                    recipient = recipient,
                    actor = null,
                    type = NotificationType.PRE_LOG_PLAN_CREATED,
                    message = "선점 테스트 알림",
                    targetId = targetId,
                    deduplicationKey = deduplicationKey,
                ),
            )
        return notificationOutboxRepository.saveAndFlush(NotificationOutbox.create(notification, deduplicationKey))
    }

    companion object {
        private const val OUTBOX_ROW_COUNT = 20
        private const val CLAIMER_COUNT = 4

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
