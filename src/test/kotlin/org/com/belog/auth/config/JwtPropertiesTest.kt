package org.com.belog.auth.config

import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.test.assertFailsWith

class JwtPropertiesTest {
    @Test
    fun `SameSite None 쿠키는 Secure 설정이 필요하다`() {
        assertFailsWith<IllegalArgumentException> {
            JwtProperties(
                secret = "test-jwt-secret-must-be-at-least-32-characters",
                issuer = "belog",
                accessTokenExpiration = Duration.ofMinutes(30),
                refreshTokenExpiration = Duration.ofDays(14),
                refreshCookieSecure = false,
                refreshCookieSameSite = "None",
            )
        }
    }
}
