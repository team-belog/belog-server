package org.com.belog.notification.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.util.Base64

@ConfigurationProperties(prefix = "fcm")
data class FcmProperties(
    val projectId: String,
    val credentialsBase64: String,
) {
    init {
        require(projectId.isNotBlank()) { "FCM 프로젝트 ID는 비어 있을 수 없습니다." }
        require(credentialsBase64.isNotBlank()) { "FCM 인증 정보는 비어 있을 수 없습니다." }
        require(runCatching { Base64.getDecoder().decode(credentialsBase64) }.isSuccess) {
            "FCM 인증 정보는 유효한 Base64 문자열이어야 합니다."
        }
    }
}
