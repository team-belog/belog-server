package org.com.belog.user.service

import org.com.belog.user.domain.SocialProvider
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserServiceTest {
    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `처음 로그인한 소셜 사용자를 생성한다`() {
        val result = login()

        assertTrue(result.isNewUser)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `동일한 소셜 사용자의 동시 최초 로그인은 모두 성공하고 한 명만 생성한다`() {
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGIN_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val loginResults =
                (1..CONCURRENT_LOGIN_COUNT).map {
                    executor.submit<SocialUserResult> {
                        startSignal.await()
                        login()
                    }
                }

            startSignal.countDown()
            val results = loginResults.map { it.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.map(SocialUserResult::userId).distinct().size)
            assertEquals(1, results.count(SocialUserResult::isNewUser))
            assertEquals(1, userRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `이미 가입한 소셜 사용자는 다시 생성하지 않는다`() {
        val firstLogin = login()
        val secondLogin = login()

        assertFalse(secondLogin.isNewUser)
        assertEquals(firstLogin.userId, secondLogin.userId)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `등록되지 않은 닉네임은 사용할 수 있다`() {
        assertTrue(userService.isNicknameAvailable("새닉네임"))
    }

    @Test
    fun `등록된 닉네임은 사용할 수 없다`() {
        login()

        assertFalse(userService.isNicknameAvailable("belog"))
    }

    private fun login(): SocialUserResult =
        userService.findOrCreateSocialUser(
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-subject",
            email = "user@example.com",
            nickname = "belog",
            profileImageUrl = "https://example.com/profile.png",
        )

    companion object {
        private const val CONCURRENT_LOGIN_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
