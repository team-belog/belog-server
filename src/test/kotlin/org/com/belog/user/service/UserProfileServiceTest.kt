package org.com.belog.user.service

import org.com.belog.global.error.BusinessException
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.S3ProfileImageObjectVerifier
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.Instant
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UserProfileServiceTest {
    private val userRepository = mock(UserRepository::class.java)
    private val profileImageService = mock(ProfileImageService::class.java)
    private val profileImageObjectVerifier = mock(S3ProfileImageObjectVerifier::class.java)
    private val userProfileUpdateTransactionService = mock(UserProfileUpdateTransactionService::class.java)
    private val service =
        UserProfileService(
            userRepository,
            profileImageService,
            profileImageObjectVerifier,
            userProfileUpdateTransactionService,
        )

    @Test
    fun `현재 사용자의 닉네임과 프로필 이미지 URL을 조회한다`() {
        val user = completedUser()
        `when`(userRepository.findById(15L)).thenReturn(Optional.of(user))
        `when`(profileImageService.resolveProfileImageUrl(user)).thenReturn("https://example.com/profile")

        val result = service.getProfile(15L)

        assertEquals("빌로그", result.nickname)
        assertEquals("https://example.com/profile", result.profileImageUrl)
        verify(profileImageService).resolveProfileImageUrl(user)
    }

    @Test
    fun `존재하지 않는 사용자의 프로필은 조회할 수 없다`() {
        `when`(userRepository.findById(999L)).thenReturn(Optional.empty())

        val exception =
            assertFailsWith<BusinessException> {
                service.getProfile(999L)
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
        verifyNoInteractions(profileImageService)
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자의 프로필은 조회할 수 없다`() {
        val user = createUser()
        `when`(userRepository.findById(15L)).thenReturn(Optional.of(user))

        val exception =
            assertFailsWith<BusinessException> {
                service.getProfile(15L)
            }

        assertEquals(UserErrorCode.ONBOARDING_REQUIRED, exception.errorCode)
        verifyNoInteractions(profileImageService)
    }

    private fun completedUser(): User =
        createUser().apply {
            completeOnboarding(
                profileImageObjectKey = null,
                nickname = "빌로그",
                name = "홍길동",
                bankAccount =
                    BankAccount.create(
                        bank = Bank.KB_KOOKMIN,
                        accountNumber = "123456789012",
                        accountHolderName = "홍길동",
                    ),
                completedAt = Instant.parse("2026-09-15T00:00:00Z"),
            )
        }

    private fun createUser(): User =
        User.createSocialUser(
            email = "user@example.com",
            provider = SocialProvider.GOOGLE,
            providerUserId = "google-subject",
            socialProfileImageUrl = "https://example.com/social-profile",
        )
}
