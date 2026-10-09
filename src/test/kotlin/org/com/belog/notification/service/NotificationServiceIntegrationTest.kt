package org.com.belog.notification.service

import org.com.belog.global.error.BusinessException
import org.com.belog.notification.code.NotificationErrorCode
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.notification.service.command.CreateNotificationCommand
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
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationServiceIntegrationTest {
    @Autowired
    private lateinit var notificationService: NotificationService

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

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
    fun `같은 중복 방지 키로 반복 생성해도 알림은 한 건만 저장된다`() {
        val recipient = saveUser("recipient")
        val command = createCommand(recipientUserId = requireNotNull(recipient.id), deduplicationKey = "plan:1:user:1")

        notificationService.create(command)
        notificationService.create(command)

        assertEquals(1L, notificationRepository.count())
        assertEquals(1L, notificationOutboxRepository.count())
    }

    @Test
    fun `중복 방지 키가 다르면 같은 대상의 알림도 여러 건 저장된다`() {
        val recipient = saveUser("recipient")
        val recipientUserId = requireNotNull(recipient.id)

        notificationService.create(createCommand(recipientUserId, "reminder:1:request:1"))
        notificationService.create(createCommand(recipientUserId, "reminder:1:request:2"))

        assertEquals(2L, notificationRepository.count())
    }

    @Test
    fun `같은 중복 방지 키로 동시에 생성해도 알림은 한 건만 저장된다`() {
        val recipient = saveUser("recipient")
        val command = createCommand(requireNotNull(recipient.id), "plan:1:user:1")
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val requests =
                (1..CONCURRENT_REQUEST_COUNT).map {
                    executor.submit {
                        startSignal.await()
                        notificationService.create(command)
                    }
                }

            startSignal.countDown()
            requests.forEach { request -> request.get(10, TimeUnit.SECONDS) }

            assertEquals(1L, notificationRepository.count())
            assertEquals(1L, notificationOutboxRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `본인의 알림만 읽음 처리할 수 있다`() {
        val recipient = saveUser("recipient")
        val otherUser = saveUser("other")
        notificationService.create(createCommand(requireNotNull(recipient.id), "plan:1:user:1"))
        val notificationId = requireNotNull(notificationRepository.findAll().single().id)

        val exception =
            assertFailsWith<BusinessException> {
                notificationService.markAsRead(notificationId, requireNotNull(otherUser.id))
            }

        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode)
        assertNull(notificationRepository.findById(notificationId).orElseThrow().readAt)
    }

    @Test
    fun `존재하지 않는 알림은 읽음 처리할 수 없다`() {
        val user = saveUser("recipient")

        val exception =
            assertFailsWith<BusinessException> {
                notificationService.markAsRead(NOT_FOUND_NOTIFICATION_ID, requireNotNull(user.id))
            }

        assertEquals(NotificationErrorCode.NOTIFICATION_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `본인 알림의 최초 읽음 시각은 반복 요청에도 유지된다`() {
        val recipient = saveUser("recipient")
        val recipientUserId = requireNotNull(recipient.id)
        notificationService.create(createCommand(recipientUserId, "plan:1:user:1"))
        val notificationId = requireNotNull(notificationRepository.findAll().single().id)
        val firstReadAt = Instant.parse("2026-10-02T00:00:00Z")
        val secondReadAt = Instant.parse("2026-10-02T01:00:00Z")
        `when`(clock.instant()).thenReturn(firstReadAt, secondReadAt)

        notificationService.markAsRead(notificationId, recipientUserId)
        notificationService.markAsRead(notificationId, recipientUserId)

        assertEquals(firstReadAt, notificationRepository.findById(notificationId).orElseThrow().readAt)
    }

    private fun saveUser(providerUserId: String): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ),
        )

    private fun createCommand(
        recipientUserId: Long,
        deduplicationKey: String,
    ): CreateNotificationCommand =
        CreateNotificationCommand(
            recipientUserId = recipientUserId,
            actorUserId = null,
            type = NotificationType.PRE_LOG_PLAN_CREATED,
            message = "새 계획이 등록됐어요",
            targetId = 1L,
            deduplicationKey = deduplicationKey,
        )

    companion object {
        private const val CONCURRENT_REQUEST_COUNT = 5
        private const val NOT_FOUND_NOTIFICATION_ID = Long.MAX_VALUE

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
