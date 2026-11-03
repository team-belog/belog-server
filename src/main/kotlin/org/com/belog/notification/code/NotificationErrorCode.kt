package org.com.belog.notification.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class NotificationErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-E001", "알림을 찾을 수 없습니다."),
    NOTIFICATION_DEVICE_NOT_FOUND(HttpStatus.NOT_FOUND, "NOTIFICATION-E002", "등록된 기기를 찾을 수 없습니다."),
}
