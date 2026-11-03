package org.com.belog.auth.config

import jakarta.servlet.http.Cookie
import org.com.belog.auth.service.AuthService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class LogoutSecurityIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jwtEncoder: JwtEncoder

    @MockitoBean
    private lateinit var authService: AuthService

    @Test
    fun `만료된 Access Token이 포함되어도 Refresh Token으로 로그아웃한다`() {
        val expiredAccessToken = createExpiredAccessToken()

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredAccessToken")
                    .cookie(Cookie("refresh_token", "refresh-token")),
            ).andExpect(status().isNoContent)
            .andExpect(cookie().maxAge("refresh_token", 0))

        verify(authService).logout("refresh-token")
    }

    @Test
    fun `만료된 Access Token이 포함되어도 허용되지 않은 Origin의 로그아웃은 거부한다`() {
        val expiredAccessToken = createExpiredAccessToken()

        mockMvc
            .perform(
                post("/api/v1/auth/logout")
                    .header(HttpHeaders.ORIGIN, "https://attacker.example")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredAccessToken")
                    .cookie(Cookie("refresh_token", "refresh-token")),
            ).andExpect(status().isForbidden)
            .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.code").value("AUTH-E006"))
            .andExpect(jsonPath("$.message").value("허용되지 않은 요청 출처입니다."))
            .andExpect(jsonPath("$.data.fieldErrors").isEmpty)
            .andExpect(jsonPath("$.data.timestamp").isNotEmpty)

        verifyNoInteractions(authService)
    }

    @Test
    fun `로그아웃 이외의 보호 API는 만료된 Access Token을 거부한다`() {
        val expiredAccessToken = createExpiredAccessToken()

        mockMvc
            .perform(
                get("/api/v1/users/me/profile")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer $expiredAccessToken"),
            ).andExpect(status().isUnauthorized)

        verifyNoInteractions(authService)
    }

    private fun createExpiredAccessToken(): String {
        val expiresAt = Instant.now().minusSeconds(60)
        val claims =
            JwtClaimsSet
                .builder()
                .issuer("belog")
                .subject("1")
                .issuedAt(expiresAt.minusSeconds(60))
                .expiresAt(expiresAt)
                .claim(JwtTokenConfig.TOKEN_TYPE_CLAIM, JwtTokenConfig.ACCESS_TOKEN_TYPE)
                .build()

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).tokenValue
    }

    companion object {
        private const val ALLOWED_ORIGIN = "http://localhost:3000"
    }
}
