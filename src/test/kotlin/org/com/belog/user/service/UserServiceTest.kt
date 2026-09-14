package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
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

    @Test
    fun `이미 사용 중인 닉네임으로 온보딩을 완료할 수 없다`() {
        val firstUserId = createUser("first-google-subject", "first@example.com")
        val secondUserId = createUser("second-google-subject", "second@example.com")
        completeOnboarding(firstUserId, "중복닉네임")

        val exception =
            assertFailsWith<BusinessException> {
                completeOnboarding(secondUserId, "중복닉네임")
            }

        assertEquals(UserErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
        assertFalse(requireNotNull(userRepository.findById(secondUserId).orElseThrow()).isOnboardingCompleted)
    }

    @Test
    fun `서로 다른 사용자가 같은 닉네임으로 동시에 온보딩하면 한 명만 성공한다`() {
        val firstUserId = createUser("first-google-subject", "first@example.com")
        val secondUserId = createUser("second-google-subject", "second@example.com")
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGIN_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val onboardingResults =
                listOf(firstUserId, secondUserId).map { userId ->
                    executor.submit<Result<Unit>> {
                        startSignal.await()
                        kotlin.runCatching { completeOnboarding(userId, "동시닉네임") }
                    }
                }

            startSignal.countDown()
            val results = onboardingResults.map { result -> result.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.count(Result<Unit>::isSuccess))
            val exception = results.single(Result<Unit>::isFailure).exceptionOrNull()
            assertIs<BusinessException>(exception)
            assertEquals(UserErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
            assertEquals(1, userRepository.findAll().count { user -> user.nickname == "동시닉네임" })
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `같은 사용자의 온보딩 요청이 동시에 실행되면 한 번만 완료한다`() {
        val userId = createUser("google-subject", "user@example.com")
        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGIN_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val onboardingResults =
                listOf("첫닉네임", "둘닉네임").map { nickname ->
                    executor.submit<Result<Unit>> {
                        startSignal.await()
                        kotlin.runCatching { completeOnboarding(userId, nickname) }
                    }
                }

            startSignal.countDown()
            val results = onboardingResults.map { result -> result.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.count(Result<Unit>::isSuccess))
            val exception = results.single(Result<Unit>::isFailure).exceptionOrNull()
            assertIs<BusinessException>(exception)
            assertEquals(UserErrorCode.ONBOARDING_ALREADY_COMPLETED, exception.errorCode)
            assertTrue(userRepository.findById(userId).orElseThrow().isOnboardingCompleted)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun completeOnboarding() {
        val user = userRepository.findAll().single()
        val userId = requireNotNull(user.id)
        completeOnboarding(userId, "belog")
    }

    private fun completeOnboarding(
        userId: Long,
        nickname: String,
    ) {
        userService.completeOnboarding(
            userId = userId,
            profileImageObjectKey = ProfileImageObjectKey.create(userId, "users/$userId/profile/image.webp"),
            nickname = nickname,
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
        )
    }

    private fun createUser(
        providerUserId: String,
        email: String,
    ): Long =
        userService
            .findOrCreateSocialUser(
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
                email = email,
            ).userId

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
