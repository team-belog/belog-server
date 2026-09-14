package org.com.belog.user.domain

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class UserTest {
    @Test
    fun `소셜 사용자 정보를 이용해 사용자를 생성한다`() {
        val user =
            User.createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            )

        assertEquals("user@example.com", user.email)
        assertEquals(null, user.nickname)
        assertEquals(null, user.profileImageObjectKey)
        assertEquals(null, user.bankAccount)
        assertFalse(user.isOnboardingCompleted)
        assertEquals(SocialProvider.GOOGLE, user.provider)
        assertEquals("google-subject", user.providerUserId)
    }

    @Test
    fun `이메일이 비어 있으면 사용자를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            User.createSocialUser(
                email = " ",
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            )
        }
    }

    @Test
    fun `소셜 사용자 식별자가 비어 있으면 사용자를 생성할 수 없다`() {
        assertFailsWith<IllegalArgumentException> {
            User.createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = " ",
            )
        }
    }

    @Test
    fun `온보딩 정보를 한 번에 저장하고 완료 상태로 변경한다`() {
        val user = createUser()
        val bankAccount =
            BankAccount.create(
                bank = Bank.KB_KOOKMIN,
                accountNumber = "123456789012",
                accountHolderName = " 홍길동 ",
            )
        val completedAt = Instant.parse("2026-09-15T00:00:00Z")

        user.completeOnboarding(
            profileImageObjectKey = " users/profile/image.webp ",
            nickname = " 빌로그 ",
            bankAccount = bankAccount,
            completedAt = completedAt,
        )

        assertEquals("users/profile/image.webp", user.profileImageObjectKey)
        assertEquals("빌로그", user.nickname)
        assertEquals(Bank.KB_KOOKMIN, user.bankAccount?.bank)
        assertEquals("123456789012", user.bankAccount?.accountNumber)
        assertEquals("홍길동", user.bankAccount?.accountHolderName)
        assertEquals(completedAt, user.onboardingCompletedAt)
        assertTrue(user.isOnboardingCompleted)
    }

    @Test
    fun `닉네임이 8자를 초과하면 온보딩을 완료할 수 없다`() {
        val user = createUser()

        assertFailsWith<IllegalArgumentException> {
            user.completeOnboarding(
                profileImageObjectKey = "users/profile/image.webp",
                nickname = "123456789",
                bankAccount = createBankAccount(),
                completedAt = Instant.parse("2026-09-15T00:00:00Z"),
            )
        }

        assertFalse(user.isOnboardingCompleted)
    }

    @Test
    fun `이미 온보딩을 완료했다면 다시 완료할 수 없다`() {
        val user = createUser()
        val completedAt = Instant.parse("2026-09-15T00:00:00Z")

        user.completeOnboarding(
            profileImageObjectKey = "users/profile/image.webp",
            nickname = "빌로그",
            bankAccount = createBankAccount(),
            completedAt = completedAt,
        )

        assertFailsWith<IllegalStateException> {
            user.completeOnboarding(
                profileImageObjectKey = "users/profile/other.webp",
                nickname = "새닉네임",
                bankAccount = createBankAccount(),
                completedAt = completedAt.plusSeconds(1),
            )
        }
    }

    private fun createUser(): User =
        User.createSocialUser(
            email = "user@example.com",
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-subject",
        )

    private fun createBankAccount(): BankAccount =
        BankAccount.create(
            bank = Bank.KB_KOOKMIN,
            accountNumber = "123456789012",
            accountHolderName = "홍길동",
        )
}
