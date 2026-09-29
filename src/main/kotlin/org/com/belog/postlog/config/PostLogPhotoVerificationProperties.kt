package org.com.belog.postlog.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

@ConfigurationProperties(prefix = "post-log.photo-verification")
data class PostLogPhotoVerificationProperties(
    val maxConcurrentHeadRequests: Int,
    val queueCapacity: Int,
    val timeout: Duration,
) {
    init {
        require(maxConcurrentHeadRequests > 0) { "Post-log 사진 동시 검증 개수는 0보다 커야 합니다." }
        require(queueCapacity > 0) { "Post-log 사진 검증 대기열 크기는 0보다 커야 합니다." }
        require(!timeout.isZero && !timeout.isNegative) { "Post-log 사진 일괄 검증 제한 시간은 0보다 커야 합니다." }
    }
}
