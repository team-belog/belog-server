package org.com.belog.prelog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "kakao.maps")
data class KakaoMapsProperties(
    val connectionTimeout: Duration,
    val readTimeout: Duration,
) {
    init {
        require(connectionTimeout.isPositive) { "Kakao Maps 연결 제한 시간은 0보다 커야 합니다." }
        require(readTimeout.isPositive) { "Kakao Maps 응답 제한 시간은 0보다 커야 합니다." }
    }
}
