package org.com.belog.global.config

import org.com.belog.auth.config.AuthCorsProcessor
import org.com.belog.auth.config.RefreshTokenOriginInterceptor
import org.com.belog.global.resolver.LoginUserIdArgumentResolver
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import tools.jackson.databind.ObjectMapper

@Configuration
@EnableConfigurationProperties(CorsProperties::class)
class WebConfig(
    private val refreshTokenOriginInterceptor: RefreshTokenOriginInterceptor,
    private val loginUserIdArgumentResolver: LoginUserIdArgumentResolver,
) : WebMvcConfigurer {
    @Bean
    fun corsConfigurationSource(properties: CorsProperties): CorsConfigurationSource {
        val configuration =
            CorsConfiguration().apply {
                allowedOrigins = properties.allowedOrigins
                allowedOriginPatterns = properties.allowedOriginPatterns
                allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                allowedHeaders = listOf("Authorization", "Content-Type", "Accept")
                allowCredentials = true
                maxAge = CORS_MAX_AGE_SECONDS
            }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", configuration)
        }
    }

    @Bean
    fun authCorsProcessor(objectMapper: ObjectMapper): AuthCorsProcessor = AuthCorsProcessor(objectMapper)

    @Bean
    fun corsFilter(
        @Qualifier("corsConfigurationSource") corsConfigurationSource: CorsConfigurationSource,
        authCorsProcessor: AuthCorsProcessor,
    ): CorsFilter =
        CorsFilter(corsConfigurationSource).apply {
            setCorsProcessor(authCorsProcessor)
        }

    @Bean
    fun corsFilterRegistration(corsFilter: CorsFilter): FilterRegistrationBean<CorsFilter> =
        FilterRegistrationBean(corsFilter).apply {
            isEnabled = false
        }

    override fun addInterceptors(registry: InterceptorRegistry) {
        registry
            .addInterceptor(refreshTokenOriginInterceptor)
            .addPathPatterns(REFRESH_TOKEN_PATH, LOGOUT_PATH)
    }

    override fun addArgumentResolvers(resolvers: MutableList<HandlerMethodArgumentResolver>) {
        resolvers.add(loginUserIdArgumentResolver)
    }

    companion object {
        private const val REFRESH_TOKEN_PATH = "/api/v1/auth/refresh"
        private const val LOGOUT_PATH = "/api/v1/auth/logout"
        private const val CORS_MAX_AGE_SECONDS = 3600L
    }
}
