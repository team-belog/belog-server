package org.com.belog.notification.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class NotificationSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    NOTIFICATIONS_RETRIEVED(HttpStatus.OK, "NOTIFICATION-S001", "알림 목록을 조회했습니다."),
}
