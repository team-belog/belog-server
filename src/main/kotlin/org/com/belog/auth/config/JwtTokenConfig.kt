package org.com.belog.auth.config

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
import java.time.Clock
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtTokenConfig {
    @Bean
    fun jwtClock(): Clock = Clock.systemUTC()

    @Bean
    fun jwtEncoder(properties: JwtProperties): JwtEncoder {
        val secretKey = secretKey(properties)
        return NimbusJwtEncoder
            .withSecretKey(secretKey)
            .algorithm(MacAlgorithm.HS256)
            .build()
    }

    @Bean
    @Qualifier(SERVICE_JWT_DECODER)
    fun serviceJwtDecoder(properties: JwtProperties): JwtDecoder {
        val decoder =
            NimbusJwtDecoder
                .withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build()
        val accessTokenValidator =
            OAuth2TokenValidator<Jwt> { jwt ->
                if (jwt.getClaimAsString(TOKEN_TYPE_CLAIM) == ACCESS_TOKEN_TYPE) {
                    OAuth2TokenValidatorResult.success()
                } else {
                    OAuth2TokenValidatorResult.failure(
                        OAuth2Error("invalid_token", "Access Token이 아닙니다.", null),
                    )
                }
            }
        decoder.setJwtValidator(
            DelegatingOAuth2TokenValidator(
                JwtValidators.createDefaultWithIssuer(properties.issuer),
                accessTokenValidator,
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
        const val SERVICE_JWT_DECODER = "serviceJwtDecoder"
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val ACCESS_TOKEN_TYPE = "access"
        const val REFRESH_TOKEN_TYPE = "refresh"
    }
}
