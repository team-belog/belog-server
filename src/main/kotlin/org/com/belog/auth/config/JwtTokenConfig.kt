package org.com.belog.auth.config

import org.com.belog.auth.infrastructure.JwtIdGenerator
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2Error
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtValidators
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.nio.charset.StandardCharsets
import java.util.UUID
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtTokenConfig {
    @Bean
    fun jwtIdGenerator(): JwtIdGenerator = JwtIdGenerator { UUID.randomUUID().toString() }

    @Bean
    fun jwtEncoder(properties: JwtProperties): JwtEncoder {
        val secretKey = secretKey(properties)
        return NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    @Qualifier(ACCESS_TOKEN_JWT_DECODER)
    fun accessTokenJwtDecoder(properties: JwtProperties): JwtDecoder =
        createDecoder(
            properties = properties,
            tokenType = ACCESS_TOKEN_TYPE,
            invalidTokenMessage = "Access Token이 아닙니다.",
        )

    @Bean
    @Qualifier(REFRESH_TOKEN_JWT_DECODER)
    fun refreshTokenJwtDecoder(properties: JwtProperties): JwtDecoder =
        createDecoder(
            properties = properties,
            tokenType = REFRESH_TOKEN_TYPE,
            invalidTokenMessage = "Refresh Token이 아닙니다.",
        )

    private fun createDecoder(
        properties: JwtProperties,
        tokenType: String,
        invalidTokenMessage: String,
    ): JwtDecoder {
        val decoder =
            NimbusJwtDecoder
                .withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
        val tokenTypeValidator =
            OAuth2TokenValidator<Jwt> { jwt ->
                if (jwt.getClaimAsString(TOKEN_TYPE_CLAIM) == tokenType) {
                    OAuth2TokenValidatorResult.success()
                } else {
                    OAuth2TokenValidatorResult.failure(
                        OAuth2Error("invalid_token", invalidTokenMessage, null),
                    )
                }
            }
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer),
                tokenTypeValidator,
            ),
        )
        return decoder
    }

    private fun secretKey(properties: JwtProperties) =
        SecretKeySpec(
            properties.secret.toByteArray(StandardCharsets.UTF_8),
            MacAlgorithm.HS256.name,
        )

    companion object {
        const val ACCESS_TOKEN_JWT_DECODER = "accessTokenJwtDecoder"
        const val REFRESH_TOKEN_JWT_DECODER = "refreshTokenJwtDecoder"
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
    }
}
