package org.com.belog.auth.config

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.global.config.CorsProperties
import org.com.belog.global.error.BusinessException
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.servlet.HandlerInterceptor

@Component
class RefreshTokenOriginInterceptor(
    corsProperties: CorsProperties,
) : HandlerInterceptor {
    private val corsConfiguration =
        CorsConfiguration().apply {
            allowedOrigins = corsProperties.allowedOrigins
            allowedOriginPatterns = corsProperties.allowedOriginPatterns
        }

    override fun preHandle(
        request: HttpServletRequest,
        response: HttpServletResponse,
        handler: Any,
    ): Boolean {
        if (!request.method.equals("POST", ignoreCase = true)) {
            return true
        }

        val origin =
            request.getHeader(HttpHeaders.ORIGIN)
                ?: throw BusinessException(AuthErrorCode.INVALID_REQUEST_ORIGIN)

        if (corsConfiguration.checkOrigin(origin) == null) {
            throw BusinessException(AuthErrorCode.INVALID_REQUEST_ORIGIN)
        }

        return true
    }
}
