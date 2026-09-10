package org.com.belog.auth.controller

import jakarta.validation.Valid
import org.com.belog.auth.code.AuthSuccessCode
import org.com.belog.auth.config.JwtProperties
import org.com.belog.auth.controller.dto.GoogleLoginRequest
import org.com.belog.auth.controller.dto.GoogleLoginResponse
import org.com.belog.auth.service.AuthService
import org.com.belog.global.response.CommonResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
class AuthController(
    private val authService: AuthService,
    private val jwtProperties: JwtProperties,
) {
    @PostMapping("/google")
    fun loginWithGoogle(
        @Valid @RequestBody request: GoogleLoginRequest,
    ): ResponseEntity<CommonResponse<GoogleLoginResponse>> {
        val result = authService.loginWithGoogle(request.authorizationCode, request.redirectUri)
        val response =
            GoogleLoginResponse(
                accessToken = result.tokens.accessToken,
                tokenType = BEARER_TOKEN_TYPE,
                expiresIn = result.tokens.accessTokenExpiration.seconds,
                onboardingRequired = result.isNewUser,
            )
        val refreshTokenCookie =
            ResponseCookie
                .from(REFRESH_TOKEN_COOKIE_NAME, result.tokens.refreshToken)
                .httpOnly(true)
                .secure(jwtProperties.refreshCookieSecure)
                .sameSite(REFRESH_TOKEN_COOKIE_SAME_SITE)
                .path(REFRESH_TOKEN_COOKIE_PATH)
                .maxAge(result.tokens.refreshTokenExpiration)
                .build()

        return ResponseEntity
            .status(AuthSuccessCode.GOOGLE_LOGIN_SUCCESS.status)
            .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
            .body(CommonResponse.success(AuthSuccessCode.GOOGLE_LOGIN_SUCCESS, response))
    }

    companion object {
        private const val BEARER_TOKEN_TYPE = "Bearer"
        private const val REFRESH_TOKEN_COOKIE_NAME = "refresh_token"
        private const val REFRESH_TOKEN_COOKIE_SAME_SITE = "Lax"
        private const val REFRESH_TOKEN_COOKIE_PATH = "/api/v1/auth/refresh"
    }
}
