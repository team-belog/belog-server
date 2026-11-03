package org.com.belog.billlog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI
import java.time.Duration

@ConfigurationProperties(prefix = "ocr.upstage")
data class UpstageReceiptExtractionProperties(
    val baseUrl: URI,
    val apiKey: String,
    val model: String,
    val mode: String,
    val connectionTimeout: Duration,
    val readTimeout: Duration,
) {
    init {
        require(baseUrl.scheme == "https" && !baseUrl.host.isNullOrBlank()) { "Upstage API 기본 URL은 유효한 HTTPS URL이어야 합니다." }
        require(apiKey.isNotBlank()) { "Upstage API Key는 비어 있을 수 없습니다." }
        require(model.isNotBlank()) { "Upstage 모델명은 비어 있을 수 없습니다." }
        require(mode == STANDARD_MODE) { "Upstage 추출 모드는 standard만 지원합니다." }
        require(connectionTimeout.isPositive) { "Upstage 연결 제한 시간은 0보다 커야 합니다." }
        require(readTimeout.isPositive) { "Upstage 응답 제한 시간은 0보다 커야 합니다." }
    }

    companion object {
        private const val STANDARD_MODE = "standard"
    }
}
