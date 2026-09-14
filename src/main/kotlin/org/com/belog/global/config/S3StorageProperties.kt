package org.com.belog.global.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "storage.s3")
data class S3StorageProperties(
    val bucket: String,
    val region: String,
    val presignExpiration: Duration,
) {
    init {
        require(bucket.isNotBlank()) { "S3 버킷 이름은 비어 있을 수 없습니다." }
        require(region.isNotBlank()) { "S3 리전은 비어 있을 수 없습니다." }
        require(!presignExpiration.isZero && !presignExpiration.isNegative) {
            "Presigned URL 만료 시간은 0보다 커야 합니다."
        }
        require(presignExpiration <= MAX_PRESIGN_EXPIRATION) {
            "Presigned URL 만료 시간은 15분 이하여야 합니다."
        }
    }

    companion object {
        private val MAX_PRESIGN_EXPIRATION: Duration = Duration.ofMinutes(15)
    }
}
