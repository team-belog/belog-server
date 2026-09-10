package org.com.belog.auth.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(GoogleAuthProperties::class)
class GoogleTokenConfig {
    @Bean
    @Qualifier(GOOGLE_OAUTH_REST_CLIENT)
    fun googleOAuthRestClient(): RestClient =
        RestClient
            .builder()
            .baseUrl(GOOGLE_OAUTH_BASE_URL)
            .build()

    @Bean
    @Qualifier(GOOGLE_JWT_DECODER)
    fun googleJwtDecoder(): JwtDecoder = NimbusJwtDecoder.withJwkSetUri(GOOGLE_JWK_SET_URI).build()

    companion object {
        const val GOOGLE_OAUTH_REST_CLIENT = "googleOAuthRestClient"
        const val GOOGLE_JWT_DECODER = "googleJwtDecoder"
        private const val GOOGLE_OAUTH_BASE_URL = "https://oauth2.googleapis.com"
        private const val GOOGLE_JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs"
    }
}
