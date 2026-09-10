package org.com.belog.auth.infrastructure

import org.com.belog.auth.config.JwtProperties
import org.com.belog.auth.config.JwtTokenConfig
import org.com.belog.auth.domain.AuthTokens
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class JwtTokenProvider(
    private val jwtEncoder: JwtEncoder,
    private val properties: JwtProperties,
    private val clock: Clock,
) {
    fun createTokens(userId: Long): AuthTokens =
        AuthTokens(
            accessToken = createToken(userId, JwtTokenConfig.ACCESS_TOKEN_TYPE, properties.accessTokenExpiration),
            refreshToken = createToken(userId, JwtTokenConfig.REFRESH_TOKEN_TYPE, properties.refreshTokenExpiration),
            accessTokenExpiration = properties.accessTokenExpiration,
            refreshTokenExpiration = properties.refreshTokenExpiration,
        )

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
