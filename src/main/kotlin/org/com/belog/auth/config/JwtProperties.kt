package org.com.belog.auth.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.time.Duration

@Validated
@ConfigurationProperties(prefix = "auth.jwt")
data class JwtProperties(
    @field:NotBlank
    @field:Size(min = 32)
    val secret: String,
    @field:NotBlank
    val issuer: String,
    val accessTokenExpiration: Duration,
    val refreshTokenExpiration: Duration,
    val refreshCookieSecure: Boolean,
) {
    init {
        require(!accessTokenExpiration.isZero && !accessTokenExpiration.isNegative) {
            "Access Token 유효시간은 0보다 커야 합니다."
        }
        require(!refreshTokenExpiration.isZero && !refreshTokenExpiration.isNegative) {
            "Refresh Token 유효시간은 0보다 커야 합니다."
        }
    }
}
