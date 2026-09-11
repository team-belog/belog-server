package org.com.belog.auth.config

import org.com.belog.auth.infrastructure.JwtTokenProvider
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtValidationException
import java.time.Clock
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class JwtTokenConfigTest {
    private val properties =
        JwtProperties(
            secret = "test-jwt-secret-must-be-at-least-32-characters",
            issuer = "belog",
            accessTokenExpiration = Duration.ofMinutes(30),
            refreshTokenExpiration = Duration.ofDays(14),
            refreshCookieSecure = false,
            refreshCookieSameSite = "Lax",
        )
    private val config = JwtTokenConfig()
    private val refreshTokenDecoder = config.refreshTokenJwtDecoder(properties)
    private val tokenProvider =
        JwtTokenProvider(
            config.jwtEncoder(properties),
            properties,
            Clock.systemUTC(),
            refreshTokenDecoder,
        )

    @Test
    fun `Refresh Token 전용 검증기는 Refresh Token을 검증한다`() {
        val refreshToken = tokenProvider.createTokens(1L).refreshToken

        val jwt = refreshTokenDecoder.decode(refreshToken)

        assertEquals("1", jwt.subject)
        assertEquals(JwtTokenConfig.REFRESH_TOKEN_TYPE, jwt.getClaimAsString(JwtTokenConfig.TOKEN_TYPE_CLAIM))
    }

    @Test
    fun `Refresh Token 전용 검증기는 Access Token을 거부한다`() {
        val accessToken = tokenProvider.createTokens(1L).accessToken

        assertFailsWith<JwtValidationException> {
            refreshTokenDecoder.decode(accessToken)
        }
    }
}
