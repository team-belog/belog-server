package org.com.belog.notification.controller.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.notification.domain.NotificationDevice

data class RegisterNotificationDeviceRequest(
    @field:NotBlank(message = "FCM 토큰을 입력해 주세요.")
    @field:Size(
        max = NotificationDevice.FCM_TOKEN_MAX_LENGTH,
        message = "FCM 토큰은 ${NotificationDevice.FCM_TOKEN_MAX_LENGTH}자를 초과할 수 없습니다.",
    )
    val fcmToken: String,
)
