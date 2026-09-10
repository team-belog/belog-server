package org.com.belog.auth.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder

@Configuration
@EnableConfigurationProperties(GoogleAuthProperties::class)
class GoogleTokenConfig {
    @Bean
    @Qualifier(GOOGLE_JWT_DECODER)
    fun googleJwtDecoder(): JwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build()

    companion object {
        const val GOOGLE_JWT_DECODER = "googleJwtDecoder"
        private const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"
    }
}
