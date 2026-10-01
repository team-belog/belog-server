package org.com.belog.notification.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class NotificationSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    NOTIFICATIONS_RETRIEVED(HttpStatus.OK, "NOTIFICATION-S001", "알림 목록을 조회했습니다."),
    PUSH_NOTIFICATION_SETTING_RETRIEVED(HttpStatus.OK, "NOTIFICATION-S002", "푸시 알림 수신 설정을 조회했습니다."),
    PUSH_NOTIFICATION_SETTING_UPDATED(HttpStatus.OK, "NOTIFICATION-S003", "푸시 알림 수신 설정을 변경했습니다."),
}
