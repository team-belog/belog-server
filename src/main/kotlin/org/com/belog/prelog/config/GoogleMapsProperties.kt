package org.com.belog.prelog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "google.maps")
data class GoogleMapsProperties(
    val apiKey: String,
    val connectionTimeout: Duration,
    val readTimeout: Duration,
    val maxRedirects: Int,
) {
    init {
        require(connectionTimeout.isPositive) { "Google Maps 연결 제한 시간은 0보다 커야 합니다." }
        require(readTimeout.isPositive) { "Google Maps 응답 제한 시간은 0보다 커야 합니다." }
        require(maxRedirects in 1..5) { "Google Maps 최대 리다이렉트 횟수는 1회 이상 5회 이하여야 합니다." }
    }
}
