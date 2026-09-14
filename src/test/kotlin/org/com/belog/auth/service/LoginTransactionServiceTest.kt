package org.com.belog.auth.service

import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.service.SocialUserResult
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class LoginTransactionServiceTest {
    private val userService = mock(UserService::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val loginTransactionService =
        LoginTransactionService(
            userService,
            jwtTokenProvider,
            refreshTokenService,
        )

    @Test
    fun `사용자 조회 생성과 토큰 발급 및 저장을 처리한다`() {
        val googleUserInfo =
            GoogleUserInfo(
                providerUserId = "google-subject",
                email = "user@example.com",
                name = "belog",
                profileImageUrl = "https://example.com/profile.png",
            )
        val tokens =
            AuthTokens(
                accessToken = "access-token",
                refreshToken = "refresh-token",
                accessTokenExpiration = Duration.ofMinutes(30),
                refreshTokenExpiration = Duration.ofDays(14),
            )
        `when`(
            userService.findOrCreateSocialUser(
                SocialProvider.GOOGLE,
                "google-subject",
                "user@example.com",
            ),
        ).thenReturn(SocialUserResult(userId = 1L, isNewUser = true))
        `when`(jwtTokenProvider.createTokens(1L)).thenReturn(tokens)

        val result = loginTransactionService.login(googleUserInfo)

        assertEquals(tokens, result.tokens)
        assertTrue(result.isNewUser)
        verify(refreshTokenService).saveOrUpdate(1L, "refresh-token", Duration.ofDays(14))
    }
}
