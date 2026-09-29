package org.com.belog.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "storage.s3")
data class S3StorageProperties(
    val bucket: String,
    val region: String,
    val presignExpiration: Duration,
    val connectionTimeout: Duration,
    val socketTimeout: Duration,
    val apiCallAttemptTimeout: Duration,
    val apiCallTimeout: Duration,
) {
    init {
        require(bucket.isNotBlank()) { "S3 버킷 이름은 비어 있을 수 없습니다." }
        require(region.isNotBlank()) { "S3 리전은 비어 있을 수 없습니다." }
        require(!presignExpiration.isZero && !presignExpiration.isNegative) {
            "Presigned URL 만료 시간은 0보다 커야 합니다."
        }
        require(connectionTimeout.isPositive) { "S3 연결 제한 시간은 0보다 커야 합니다." }
        require(socketTimeout.isPositive) { "S3 소켓 제한 시간은 0보다 커야 합니다." }
        require(apiCallAttemptTimeout.isPositive) { "S3 API 호출 시도 제한 시간은 0보다 커야 합니다." }
        require(apiCallTimeout.isPositive) { "S3 API 호출 전체 제한 시간은 0보다 커야 합니다." }
        require(apiCallAttemptTimeout <= apiCallTimeout) {
            "S3 API 호출 시도 제한 시간은 전체 제한 시간을 초과할 수 없습니다."
        }
    }
}
