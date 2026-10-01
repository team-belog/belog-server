package org.com.belog.notification.controller.dto.request

import jakarta.validation.constraints.NotNull

data class UpdatePushNotificationSettingRequest(
    @field:NotNull(message = "푸시 알림 수신 여부는 필수입니다.")
    val pushNotificationEnabled: Boolean?,
)
