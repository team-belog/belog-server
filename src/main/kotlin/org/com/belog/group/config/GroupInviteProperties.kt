package org.com.belog.group.config

import org.springframework.boot.context.properties.ConfigurationProperties
import java.net.URI

@ConfigurationProperties(prefix = "group.invite")
data class GroupInviteProperties(
    val baseUrl: URI,
) {
    init {
        require(baseUrl.isAbsolute && baseUrl.host != null) { "그룹 초대 링크 기본 URL은 절대 URL이어야 합니다." }
        require(baseUrl.scheme.equals("http", ignoreCase = true) || baseUrl.scheme.equals("https", ignoreCase = true)) {
            "그룹 초대 링크 기본 URL은 HTTP 또는 HTTPS URL이어야 합니다."
        }
        require(baseUrl.query == null && baseUrl.fragment == null) {
            "그룹 초대 링크 기본 URL에는 쿼리나 fragment를 포함할 수 없습니다."
        }
    }
}
