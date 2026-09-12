package org.com.belog.auth.config

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        @Qualifier(JwtTokenConfig.ACCESS_TOKEN_JWT_DECODER) accessTokenJwtDecoder: JwtDecoder,
        bearerAuthenticationEntryPoint: BearerAuthenticationEntryPoint,
    ): SecurityFilterChain =
        http
            .csrf { csrf -> csrf.disable() }
            .cors { }
            .sessionManagement { session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { requests ->
                requests
                    .requestMatchers(
                        "/api/v1/auth/google",
                        "/api/v1/auth/refresh",
                        "/actuator/health",
                        "/v3/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                    ).permitAll()
                    .anyRequest()
                    .authenticated()
            }.exceptionHandling { exceptions ->
                exceptions.authenticationEntryPoint(bearerAuthenticationEntryPoint)
            }.oauth2ResourceServer { resourceServer ->
                resourceServer
                    .authenticationEntryPoint(bearerAuthenticationEntryPoint)
                    .jwt { jwt -> jwt.decoder(accessTokenJwtDecoder) }
            }.build()
}
