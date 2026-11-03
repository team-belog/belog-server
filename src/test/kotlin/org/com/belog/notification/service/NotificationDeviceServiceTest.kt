package org.com.belog.notification.service

import org.com.belog.notification.repository.NotificationDeviceRepository
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationDeviceServiceTest {
    @Autowired
    private lateinit var notificationDeviceService: NotificationDeviceService

    @Autowired
    private lateinit var notificationDeviceRepository: NotificationDeviceRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        notificationDeviceRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `같은 FCM 토큰이 다른 사용자에게 재등록되면 기존 등록은 비활성화된다`() {
        val userA = saveUser("user-a")
        val userB = saveUser("user-b")
        notificationDeviceService.registerDevice(requireNotNull(userA.id), "device-a", "shared-token")

        notificationDeviceService.registerDevice(requireNotNull(userB.id), "device-b", "shared-token")

        val userADevice = notificationDeviceRepository.findByUserIdAndDeviceId(requireNotNull(userA.id), "device-a")
        val userBDevice = notificationDeviceRepository.findByUserIdAndDeviceId(requireNotNull(userB.id), "device-b")
        assertFalse(requireNotNull(userADevice).isActive)
        assertTrue(requireNotNull(userBDevice).isActive)
        assertEquals("shared-token", userBDevice.fcmToken)
    }

    @Test
    fun `같은 기기의 토큰을 다시 등록해도 자기 자신은 비활성화되지 않는다`() {
        val user = saveUser("user-a")
        notificationDeviceService.registerDevice(requireNotNull(user.id), "device-a", "token-1")

        notificationDeviceService.registerDevice(requireNotNull(user.id), "device-a", "token-2")

        val activeDevices = notificationDeviceRepository.findAllByUserIdAndDeactivatedAtIsNull(requireNotNull(user.id))
        assertEquals(1, activeDevices.size)
        assertEquals("token-2", activeDevices.single().fcmToken)
    }

    private fun saveUser(providerUserId: String): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ),
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
