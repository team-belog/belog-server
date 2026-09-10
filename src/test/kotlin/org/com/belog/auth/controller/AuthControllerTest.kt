package org.com.belog.auth.controller

import org.com.belog.auth.config.JwtProperties
import org.com.belog.auth.domain.AuthTokens
import org.com.belog.auth.domain.GoogleLoginResult
import org.com.belog.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Duration

@WebMvcTest(AuthController::class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var authService: AuthService

    @MockitoBean
    private lateinit var jwtProperties: JwtProperties

    @Test
    fun `Google 로그인에 성공하면 Access Token과 Refresh Token 쿠키를 반환한다`() {
        `when`(jwtProperties.refreshCookieSecure).thenReturn(false)
        `when`(
            authService.loginWithGoogle(
                "google-authorization-code",
                "http://localhost:3000/oauth/google/callback",
            ),
        ).thenReturn(
            GoogleLoginResult(
                tokens =
                    AuthTokens(
                        accessToken = "access-token",
                        refreshToken = "refresh-token",
                        accessTokenExpiration = Duration.ofMinutes(30),
                        refreshTokenExpiration = Duration.ofDays(14),
                    ),
                isNewUser = true,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"authorizationCode":"google-authorization-code","redirectUri":"http://localhost:3000/oauth/google/callback"}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("AUTH-S001"))
            .andExpect(jsonPath("$.data.accessToken").value("access-token"))
            .andExpect(jsonPath("$.data.tokenType").doesNotExist())
            .andExpect(jsonPath("$.data.expiresIn").value(1800))
            .andExpect(jsonPath("$.data.onboardingRequired").value(true))
            .andExpect(jsonPath("$.data.isNewUser").doesNotExist())
            .andExpect(cookie().value("refresh_token", "refresh-token"))
            .andExpect(cookie().httpOnly("refresh_token", true))
            .andExpect(cookie().path("refresh_token", "/api/v1/auth/refresh"))
    }

    @Test
    fun `Google 인가 코드가 비어 있으면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/auth/google")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"authorizationCode":"","redirectUri":"http://localhost:3000/oauth/google/callback"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))
    }
}
