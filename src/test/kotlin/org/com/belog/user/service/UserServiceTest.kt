package org.com.belog.user.service

import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
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
import java.time.Instant
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

        assertTrue(result.onboardingRequired)
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
            assertTrue(results.all(SocialUserResult::onboardingRequired))
            assertEquals(1, userRepository.count())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `온보딩을 완료하지 않은 소셜 사용자는 재로그인해도 온보딩이 필요하다`() {
        val firstLogin = login()
        val secondLogin = login()

        assertTrue(secondLogin.onboardingRequired)
        assertEquals(firstLogin.userId, secondLogin.userId)
        assertEquals(1, userRepository.count())
    }

    @Test
    fun `온보딩을 완료한 소셜 사용자는 재로그인할 때 온보딩이 필요하지 않다`() {
        login()
        completeOnboarding()

        val result = login()

        assertFalse(result.onboardingRequired)
    }

    @Test
    fun `등록되지 않은 닉네임은 사용할 수 있다`() {
        assertTrue(userService.isNicknameAvailable("새닉네임"))
    }

    @Test
    fun `등록된 닉네임은 사용할 수 없다`() {
        login()
        completeOnboarding()

        assertFalse(userService.isNicknameAvailable("belog"))
    }

    private fun completeOnboarding() {
        val user = userRepository.findAll().single()
        val userId = requireNotNull(user.id)
        user.completeOnboarding(
            profileImageObjectKey = ProfileImageObjectKey.create(userId, "users/$userId/profile/image.webp"),
            nickname = "belog",
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-15T00:00:00Z"),
        )
        userRepository.saveAndFlush(user)
    }

    private fun login(): SocialUserResult =
        userService.findOrCreateSocialUser(
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-subject",
            email = "user@example.com",
        )

    companion object {
        private const val CONCURRENT_LOGIN_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
