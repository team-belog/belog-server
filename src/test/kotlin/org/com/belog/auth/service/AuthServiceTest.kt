package org.com.belog.auth.service

import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleLoginResult
import org.com.belog.auth.domain.GoogleUserInfo
import org.com.belog.auth.infrastructure.GoogleAuthorizationCodeClient
import org.com.belog.auth.infrastructure.GoogleIdTokenVerifier
import org.com.belog.auth.infrastructure.JwtTokenProvider
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
    private val loginTransactionService = mock(LoginTransactionService::class.java)
    private val jwtTokenProvider = mock(JwtTokenProvider::class.java)
    private val refreshTokenService = mock(RefreshTokenService::class.java)
    private val authService =
        AuthService(
            googleAuthorizationCodeClient,
            googleIdTokenVerifier,
            loginTransactionService,
            jwtTokenProvider,
            refreshTokenService,
        )

    @Test
    fun `Google 사용자를 등록하고 서비스 토큰을 발급한다`() {
        val googleUserInfo =
            GoogleUserInfo(
                providerUserId = "google-subject",
                email = "user@example.com",
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
        val loginResult = GoogleLoginResult(tokens = tokens, onboardingRequired = true)
        `when`(loginTransactionService.login(googleUserInfo)).thenReturn(loginResult)

        val result =
            authService.loginWithGoogle(
                "google-authorization-code",
                "http://localhost:3000/oauth/google/callback",
            )

        assertEquals(tokens, result.tokens)
        assertTrue(result.onboardingRequired)
        verify(googleIdTokenVerifier).verify("google-id-token")
        verify(loginTransactionService).login(googleUserInfo)
    }

    @Test
    fun `유효한 Refresh Token으로 새로운 토큰을 발급한다`() {
        val tokens =
            AuthTokens(
                accessToken = "new-access-token",
                refreshToken = "new-refresh-token",
                accessTokenExpiration = Duration.ofMinutes(30),
                refreshTokenExpiration = Duration.ofDays(14),
            )
        `when`(jwtTokenProvider.extractUserIdFromRefreshToken("current-refresh-token")).thenReturn(1L)
        `when`(jwtTokenProvider.createTokens(1L)).thenReturn(tokens)

        val result = authService.reissueTokens("current-refresh-token")

        assertEquals(tokens, result)
        verify(refreshTokenService).validateAndRotate(
            userId = 1L,
            currentRefreshToken = "current-refresh-token",
            newRefreshToken = "new-refresh-token",
            expiration = Duration.ofDays(14),
        )
    }
}
