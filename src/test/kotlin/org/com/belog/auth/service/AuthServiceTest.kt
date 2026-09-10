package org.com.belog.auth.service

import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.infrastructure.GoogleAuthorizationCodeClient
import org.com.belog.auth.infrastructure.GoogleIdTokenVerifier
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

class AuthServiceTest {
    private val googleAuthorizationCodeClient = mock(GoogleAuthorizationCodeClient::class.java)
    private val googleIdTokenVerifier = mock(GoogleIdTokenVerifier::class.java)
    private val userService = mock(UserService::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val authService =
        AuthService(
            googleAuthorizationCodeClient,
            googleIdTokenVerifier,
            userService,
            jwtTokenProvider,
        )

    @Test
    fun `Google 사용자를 등록하고 서비스 토큰을 발급한다`() {
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
            googleAuthorizationCodeClient.exchangeForIdToken(
                "google-authorization-code",
                "http://localhost:3000/oauth/google/callback",
            ),
        ).thenReturn("google-id-token")
        `when`(googleIdTokenVerifier.verify("google-id-token")).thenReturn(googleUserInfo)
        `when`(
            userService.findOrCreateSocialUser(
                SocialProvider.GOOGLE,
                "google-subject",
                "user@example.com",
                "belog",
                "https://example.com/profile.png",
            ),
        ).thenReturn(SocialUserResult(userId = 1L, isNewUser = true))
        `when`(jwtTokenProvider.createTokens(1L)).thenReturn(tokens)

        val result =
            authService.loginWithGoogle(
                "google-authorization-code",
                "http://localhost:3000/oauth/google/callback",
            )

        assertEquals(tokens, result.tokens)
        assertTrue(result.isNewUser)
        verify(googleIdTokenVerifier).verify("google-id-token")
        verify(jwtTokenProvider).createTokens(1L)
    }
}
