package org.com.belog.auth.infrastructure

import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.config.JwtProperties
import org.com.belog.auth.config.JwtTokenConfig
import org.com.belog.auth.domain.AuthTokens
import org.com.belog.global.error.BusinessException
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.security.oauth2.jwt.JwtException
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val properties: JwtProperties,
    private val clock: Clock,
    @Qualifier(JwtTokenConfig.REFRESH_TOKEN_JWT_DECODER)
    private val refreshTokenJwtDecoder: JwtDecoder,
) {
    fun createTokens(userId: Long): AuthTokens =
        AuthTokens(
            accessToken = createToken(userId, JwtTokenConfig.ACCESS_TOKEN_TYPE, properties.accessTokenExpiration),
            refreshToken = createToken(userId, JwtTokenConfig.REFRESH_TOKEN_TYPE, properties.refreshTokenExpiration),
            accessTokenExpiration = properties.accessTokenExpiration,
            refreshTokenExpiration = properties.refreshTokenExpiration,
        )

    fun extractUserIdFromRefreshToken(refreshToken: String): Long {
        if (refreshToken.isBlank()) {
            throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
        }

        val jwt =
            try {
                refreshTokenJwtDecoder.decode(refreshToken)
            } catch (exception: JwtException) {
                throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN, exception)
            }

        return jwt.subject
            ?.toLongOrNull()
            ?.takeIf { userId -> userId > 0 }
            ?: throw BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN)
    }

    private fun createToken(
        userId: Long,
        tokenType: String,
        expiration: Duration,
    ): String {
        val issuedAt = Instant.now(clock)
        val claims =
            JwtClaimsSet
                .builder()
                .issuer(properties.issuer)
                .subject(userId.toString())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(expiration))
                .claim(JwtTokenConfig.TOKEN_TYPE_CLAIM, tokenType)
                .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }
}
