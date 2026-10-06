package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.ProfileImageSource
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.ProfileImageStorage
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.command.ProfileImageChange
import org.com.belog.user.service.result.SocialUserResult
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.dao.DataAccessException
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.TransactionDefinition
import org.springframework.transaction.support.TransactionTemplate
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
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

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var transactionManager: PlatformTransactionManager

    @MockitoBean
    private lateinit var profileImageStorage: ProfileImageStorage

    @AfterEach
    fun cleanUp() {
        userRepository.deleteAll()
    }

    @Test
    fun `처음 로그인한 소셜 사용자를 생성한다`() {
        val result = login()

        assertTrue(result.onboardingRequired)
        assertEquals(1, userRepository.count())
        assertEquals(ProfileImageSource.SOCIAL, userRepository.findById(result.userId).orElseThrow().profileImageSource)
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
    fun `Google 프로필 이미지 URL을 저장하고 재로그인할 때 최신 값으로 갱신한다`() {
        val userId =
            userService
                .findOrCreateSocialUser(
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "google-subject",
                    email = "user@example.com",
                    socialProfileImageUrl = "https://lh3.googleusercontent.com/old-profile",
                ).userId

        val result =
            userService.findOrCreateSocialUser(
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
                email = "user@example.com",
                socialProfileImageUrl = "https://lh3.googleusercontent.com/new-profile",
            )

        assertEquals("https://lh3.googleusercontent.com/new-profile", result.socialProfileImageUrl)
        assertEquals(
            "https://lh3.googleusercontent.com/new-profile",
            userRepository.findById(userId).orElseThrow().socialProfileImageUrl,
        )
    }

    @Test
    fun `프로필 이미지를 직접 등록하지 않아도 온보딩을 완료한다`() {
        val userId = createUser("google-subject", "user@example.com")

        userService.completeOnboarding(
            userId = userId,
            profileImageObjectKey = null,
            nickname = "belog",
            name = " 홍 길동 ",
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
        )

        val user = userRepository.findById(userId).orElseThrow()
        assertEquals(null, user.profileImageObjectKey)
        assertEquals("홍 길동", user.name)
        assertTrue(user.isOnboardingCompleted)
    }

    @Test
    fun `온보딩 완료 사용자의 이름을 null로 변경할 수 없다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "belog")

        assertFailsWith<DataAccessException> {
            jdbcTemplate.update("UPDATE users SET name = NULL WHERE id = ?", userId)
        }
    }

    @Test
    fun `등록된 닉네임은 사용할 수 없다`() {
        login()
        completeOnboarding()

        assertFalse(userService.isNicknameAvailable("belog"))
    }

    @Test
    fun `계좌번호는 데이터베이스에 암호화해서 저장한다`() {
        val userId = createUser("google-subject", "user@example.com")

        completeOnboarding(userId, "belog")

        val encryptedAccountNumber =
            jdbcTemplate.queryForObject(
                "SELECT encrypted_account_number FROM users WHERE id = ?",
                String::class.java,
                userId,
            )
        assertNotNull(encryptedAccountNumber)
        assertNotEquals("123456789012", encryptedAccountNumber)
        assertTrue(encryptedAccountNumber.startsWith("v1."))
    }

    @Test
    fun `등록된 계좌를 새로운 계좌로 수정한다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "belog")

        userService.updateBankAccount(
            userId = userId,
            bankAccount =
                BankAccount.create(
                    bank = Bank.SHINHAN,
                    accountNumber = "110123456789",
                    accountHolderName = "김빌로그",
                ),
        )

        val updatedBankAccount = requireNotNull(userRepository.findById(userId).orElseThrow().bankAccount)
        assertEquals(Bank.SHINHAN, updatedBankAccount.bank)
        assertEquals("110123456789", updatedBankAccount.accountNumber)
        assertEquals("김빌로그", updatedBankAccount.accountHolderName)
    }

    @Test
    fun `수정한 계좌번호는 암호화해서 저장한다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "belog")
        val updatedAccountNumber = "110123456789"

        userService.updateBankAccount(
            userId = userId,
            bankAccount =
                BankAccount.create(
                    bank = Bank.SHINHAN,
                    accountNumber = updatedAccountNumber,
                    accountHolderName = "홍길동",
                ),
        )

        val encryptedAccountNumber =
            jdbcTemplate.queryForObject(
                "SELECT encrypted_account_number FROM users WHERE id = ?",
                String::class.java,
                userId,
            )
        assertNotNull(encryptedAccountNumber)
        assertNotEquals(updatedAccountNumber, encryptedAccountNumber)
        assertTrue(encryptedAccountNumber.startsWith("v1."))
        val savedUser = userRepository.findById(userId).orElseThrow()
        assertEquals(updatedAccountNumber, savedUser.bankAccount?.accountNumber)
    }

    @Test
    fun `존재하지 않는 사용자의 계좌를 수정할 수 없다`() {
        val exception =
            assertFailsWith<BusinessException> {
                userService.updateBankAccount(
                    userId = 999L,
                    bankAccount =
                        BankAccount.create(
                            bank = Bank.SHINHAN,
                            accountNumber = "110123456789",
                            accountHolderName = "홍길동",
                        ),
                )
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
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
    fun `탈퇴한 사용자의 닉네임과 소셜 식별자를 신규 사용자가 재사용한다`() {
        val withdrawnUserId = createUser("reusable-google-subject", "withdrawn@example.com")
        completeOnboarding(withdrawnUserId, "재사용닉네임")
        withdrawUser(withdrawnUserId)

        assertTrue(userService.isNicknameAvailable("재사용닉네임"))
        assertEquals(
            null,
            userRepository.findActiveSocialUserId(
                provider = SocialProvider.GOOGLE,
                providerUserId = "reusable-google-subject",
            ),
        )

        val newUserId = createUser("reusable-google-subject", "new@example.com")
        completeOnboarding(newUserId, "재사용닉네임")

        assertNotEquals(withdrawnUserId, newUserId)
        val withdrawnUser = userRepository.findById(withdrawnUserId).orElseThrow()
        assertFalse(withdrawnUser.isActive)
        assertEquals("withdrawn@example.com", withdrawnUser.email)
        assertEquals("reusable-google-subject", withdrawnUser.providerUserId)
        assertEquals("재사용닉네임", withdrawnUser.nickname)
        assertEquals(
            newUserId,
            userRepository.findActiveSocialUserId(
                provider = SocialProvider.GOOGLE,
                providerUserId = "reusable-google-subject",
            ),
        )
        assertFalse(userService.isNicknameAvailable("재사용닉네임"))
    }

    @Test
    fun `탈퇴 후 동일 소셜 식별자로 동시에 로그인하면 신규 사용자 한 명만 생성된다`() {
        val withdrawnUserId = createUser("concurrent-reuse-google-subject", "withdrawn@example.com")
        withdrawUser(withdrawnUserId)

        val executor = Executors.newFixedThreadPool(CONCURRENT_LOGIN_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val logins =
                (1..CONCURRENT_LOGIN_COUNT).map {
                    executor.submit<SocialUserResult> {
                        startSignal.await()
                        userService.findOrCreateSocialUser(
                            provider = SocialProvider.GOOGLE,
                            providerUserId = "concurrent-reuse-google-subject",
                            email = "new@example.com",
                        )
                    }
                }

            startSignal.countDown()
            val results = logins.map { login -> login.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.map(SocialUserResult::userId).distinct().size)
            assertTrue(results.all(SocialUserResult::onboardingRequired))
            assertTrue(results.none { result -> result.userId == withdrawnUserId })
            assertEquals(2, userRepository.count())
            assertFalse(userRepository.findById(withdrawnUserId).orElseThrow().isActive)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `탈퇴 커밋 직후 로그인은 탈퇴 계정을 재사용하지 않고 신규 사용자를 만든다`() {
        val withdrawnUserId = createUser("race-google-subject", "race@example.com")
        val executor = Executors.newFixedThreadPool(1)
        val withdrawReadyToCommit = CountDownLatch(1)
        val loginStarted = CountDownLatch(1)
        val withdrawTransactionTemplate = TransactionTemplate(transactionManager)
        withdrawTransactionTemplate.propagationBehavior = TransactionDefinition.PROPAGATION_REQUIRES_NEW

        try {
            val loginFuture =
                executor.submit<SocialUserResult> {
                    withdrawReadyToCommit.await()
                    loginStarted.countDown()
                    userService.findOrCreateSocialUser(
                        provider = SocialProvider.GOOGLE,
                        providerUserId = "race-google-subject",
                        email = "new@example.com",
                    )
                }

            withdrawTransactionTemplate.executeWithoutResult {
                val user = userRepository.findByIdForUpdate(withdrawnUserId)!!
                user.withdraw(Instant.parse("2026-10-07T00:00:00Z"))
                userRepository.saveAndFlush(user)
                withdrawReadyToCommit.countDown()
                assertTrue(loginStarted.await(5, TimeUnit.SECONDS))
                Thread.sleep(LOGIN_LOCK_WAIT_MILLIS)
            }

            val result = loginFuture.get(10, TimeUnit.SECONDS)

            assertNotEquals(withdrawnUserId, result.userId)
            assertTrue(result.onboardingRequired)
            assertEquals(2, userRepository.count())
            assertFalse(userRepository.findById(withdrawnUserId).orElseThrow().isActive)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun `탈퇴 후 재사용된 닉네임은 다른 활성 사용자가 중복 사용할 수 없다`() {
        val withdrawnUserId = createUser("withdrawn-google-subject", "withdrawn@example.com")
        completeOnboarding(withdrawnUserId, "재사용닉네임")
        withdrawUser(withdrawnUserId)
        val activeUserId = createUser("active-google-subject", "active@example.com")
        completeOnboarding(activeUserId, "재사용닉네임")
        val duplicateUserId = createUser("duplicate-google-subject", "duplicate@example.com")

        val exception =
            assertFailsWith<BusinessException> {
                completeOnboarding(duplicateUserId, "재사용닉네임")
            }

        assertEquals(UserErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
        assertFalse(userRepository.findById(duplicateUserId).orElseThrow().isOnboardingCompleted)
    }

    @Test
    fun `탈퇴 후 재사용된 소셜 식별자는 다른 활성 사용자가 중복 사용할 수 없다`() {
        val withdrawnUserId = createUser("reusable-google-subject", "withdrawn@example.com")
        withdrawUser(withdrawnUserId)
        createUser("reusable-google-subject", "active@example.com")

        assertFailsWith<DataIntegrityViolationException> {
            userRepository.saveAndFlush(
                User.createSocialUser(
                    email = "duplicate@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "reusable-google-subject",
                ),
            )
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

    @Test
    fun `닉네임만 변경하면 기존 프로필 이미지를 유지한다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "기존닉네임")
        val existingObjectKey = "users/$userId/profile/image.webp"

        userService.updateProfile(
            userId = userId,
            nickname = "새닉네임",
            profileImageChange = null,
        )

        val updatedUser = userRepository.findById(userId).orElseThrow()
        assertEquals("새닉네임", updatedUser.nickname)
        assertEquals(ProfileImageSource.CUSTOM, updatedUser.profileImageSource)
        assertEquals(existingObjectKey, updatedUser.profileImageObjectKey)
    }

    @Test
    fun `업로드한 이미지로 프로필 이미지를 변경한다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "기존닉네임")
        val newObjectKey = ProfileImageObjectKey.create(userId, "users/$userId/profile/new-image.webp")

        userService.updateProfile(
            userId = userId,
            nickname = null,
            profileImageChange = ProfileImageChange.Update(newObjectKey),
        )

        val updatedUser = userRepository.findById(userId).orElseThrow()
        verify(profileImageStorage).verify(newObjectKey)
        assertEquals(ProfileImageSource.CUSTOM, updatedUser.profileImageSource)
        assertEquals(newObjectKey.value, updatedUser.profileImageObjectKey)
    }

    @Test
    fun `프로필 이미지를 앱 기본 이미지로 변경한다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "기존닉네임")

        userService.updateProfile(
            userId = userId,
            nickname = null,
            profileImageChange = ProfileImageChange.Reset,
        )

        val updatedUser = userRepository.findById(userId).orElseThrow()
        assertEquals(ProfileImageSource.DEFAULT, updatedUser.profileImageSource)
        assertEquals(null, updatedUser.profileImageObjectKey)
    }

    @Test
    fun `중복 닉네임과 이미지 변경을 함께 요청하면 모든 변경을 롤백한다`() {
        val firstUserId = createUser("first-google-subject", "first@example.com")
        val secondUserId = createUser("second-google-subject", "second@example.com")
        completeOnboarding(firstUserId, "중복닉네임")
        completeOnboarding(secondUserId, "기존닉네임")
        val existingObjectKey = "users/$secondUserId/profile/image.webp"

        val exception =
            assertFailsWith<BusinessException> {
                userService.updateProfile(
                    userId = secondUserId,
                    nickname = "중복닉네임",
                    profileImageChange = ProfileImageChange.Reset,
                )
            }

        val unchangedUser = userRepository.findById(secondUserId).orElseThrow()
        assertEquals(UserErrorCode.NICKNAME_ALREADY_EXISTS, exception.errorCode)
        assertEquals("기존닉네임", unchangedUser.nickname)
        assertEquals(ProfileImageSource.CUSTOM, unchangedUser.profileImageSource)
        assertEquals(existingObjectKey, unchangedUser.profileImageObjectKey)
    }

    @Test
    fun `S3 이미지 검증에 실패하면 프로필을 변경하지 않는다`() {
        val userId = createUser("google-subject", "user@example.com")
        completeOnboarding(userId, "기존닉네임")
        val existingObjectKey = "users/$userId/profile/image.webp"
        val newObjectKey = ProfileImageObjectKey.create(userId, "users/$userId/profile/new-image.webp")
        doThrow(BusinessException(UserErrorCode.PROFILE_IMAGE_NOT_FOUND))
            .`when`(profileImageStorage)
            .verify(newObjectKey)

        val exception =
            assertFailsWith<BusinessException> {
                userService.updateProfile(
                    userId = userId,
                    nickname = "새닉네임",
                    profileImageChange = ProfileImageChange.Update(newObjectKey),
                )
            }

        val unchangedUser = userRepository.findById(userId).orElseThrow()
        assertEquals(UserErrorCode.PROFILE_IMAGE_NOT_FOUND, exception.errorCode)
        assertEquals("기존닉네임", unchangedUser.nickname)
        assertEquals(ProfileImageSource.CUSTOM, unchangedUser.profileImageSource)
        assertEquals(existingObjectKey, unchangedUser.profileImageObjectKey)
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자는 프로필을 변경할 수 없다`() {
        val userId = createUser("google-subject", "user@example.com")

        val exception =
            assertFailsWith<BusinessException> {
                userService.updateProfile(
                    userId = userId,
                    nickname = "새닉네임",
                    profileImageChange = null,
                )
            }

        val unchangedUser = userRepository.findById(userId).orElseThrow()
        assertEquals(UserErrorCode.ONBOARDING_REQUIRED, exception.errorCode)
        assertEquals(null, unchangedUser.nickname)
        assertEquals(ProfileImageSource.SOCIAL, unchangedUser.profileImageSource)
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
            name = "홍길동",
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

    private fun withdrawUser(userId: Long) {
        val user = userRepository.findById(userId).orElseThrow()
        user.withdraw(Instant.parse("2026-10-07T00:00:00Z"))
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
        private const val LOGIN_LOCK_WAIT_MILLIS = 200L

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
