package org.com.belog.auth.infrastructure

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.config.JwtProperties
import org.com.belog.auth.config.JwtTokenConfig
import org.com.belog.global.error.BusinessException
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class JwtTokenProviderTest {
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
    private val jwtEncoder = config.jwtEncoder(properties)
    private val clock = Clock.fixed(Instant.now(), ZoneOffset.UTC)
    private var jwtIdSequence = 0
    private val tokenProvider =
        JwtTokenProvider(
            jwtEncoder = jwtEncoder,
            properties = properties,
            clock = clock,
            jwtIdGenerator = JwtIdGenerator { "refresh-token-id-${++jwtIdSequence}" },
            refreshTokenJwtDecoder = config.refreshTokenJwtDecoder(properties),
        )

    @Test
    fun `같은 시각에 발급한 Refresh Token은 jti로 구분한다`() {
        val firstTokens = tokenProvider.createTokens(1L)
        val secondTokens = tokenProvider.createTokens(1L)

        assertNotEquals(firstTokens.refreshToken, secondTokens.refreshToken)
    }

    @Test
    fun `Access Token에는 jti를 추가하지 않는다`() {
        val accessToken = tokenProvider.createTokens(1L).accessToken

        val jwt = config.accessTokenJwtDecoder(properties).decode(accessToken)

        assertNull(jwt.id)
    }

    @Test
    fun `Refresh Token에서 사용자 ID를 추출한다`() {
        val refreshToken = tokenProvider.createTokens(1L).refreshToken

        val userId = tokenProvider.extractUserIdFromRefreshToken(refreshToken)

        assertEquals(1L, userId)
    }

    @Test
    fun `Refresh Token 전용 사용자 ID 추출은 Access Token을 거부한다`() {
        val accessToken = tokenProvider.createTokens(1L).accessToken

        val exception =
            assertFailsWith<BusinessException> {
                tokenProvider.extractUserIdFromRefreshToken(accessToken)
            }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }

    @Test
    fun `사용자 ID 형식이 올바르지 않으면 추출할 수 없다`() {
        val issuedAt = Instant.now()
        val claims =
            JwtClaimsSet
                .builder()
                .issuer(properties.issuer)
                .subject("invalid-user-id")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.refreshTokenExpiration))
                .claim(JwtTokenConfig.TOKEN_TYPE_CLAIM, JwtTokenConfig.REFRESH_TOKEN_TYPE)
                .build()
        val refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue

        val exception =
            assertFailsWith<BusinessException> {
                tokenProvider.extractUserIdFromRefreshToken(refreshToken)
            }

        assertEquals(AuthErrorCode.INVALID_REFRESH_TOKEN, exception.errorCode)
    }
}
