package org.com.belog.notification.controller

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.notification.code.NotificationSuccessCode
import org.com.belog.notification.controller.dto.request.UpdatePushNotificationSettingRequest
import org.com.belog.notification.controller.dto.response.NotificationListResponse
import org.com.belog.notification.controller.dto.response.PushNotificationSettingResponse
import org.com.belog.notification.controller.swagger.NotificationSwagger
import org.com.belog.notification.service.NotificationQueryService
import org.com.belog.notification.service.NotificationService
import org.springframework.http.CacheControl
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
    private val notificationService: NotificationService,
) : NotificationSwagger {
    @GetMapping("/settings")
    override fun getPushNotificationSetting(
        @LoginUserId userId: Long,
    ): ResponseEntity<CommonResponse<PushNotificationSettingResponse>> {
        val response =
            PushNotificationSettingResponse(
                pushNotificationEnabled = notificationService.getPushNotificationEnabled(userId),
            )

        return ResponseEntity
            .status(NotificationSuccessCode.PUSH_NOTIFICATION_SETTING_RETRIEVED.status)
            .cacheControl(CacheControl.noStore())
            .body(
                CommonResponse.success(
                    NotificationSuccessCode.PUSH_NOTIFICATION_SETTING_RETRIEVED,
                    response,
                ),
            )
    }

    @PutMapping("/settings")
    override fun updatePushNotificationSetting(
        @LoginUserId userId: Long,
        @Valid @RequestBody request: UpdatePushNotificationSettingRequest,
    ): ResponseEntity<CommonResponse<Nothing>> {
        notificationService.updatePushNotificationEnabled(
            userId = userId,
            enabled = requireNotNull(request.pushNotificationEnabled),
        )

        return ResponseEntity
            .status(NotificationSuccessCode.PUSH_NOTIFICATION_SETTING_UPDATED.status)
            .body(CommonResponse.success(NotificationSuccessCode.PUSH_NOTIFICATION_SETTING_UPDATED))
    }

    @GetMapping
    override fun getNotifications(
        @LoginUserId userId: Long,
        @RequestParam(required = false) @Positive cursor: Long?,
        @RequestParam(defaultValue = "20") @Min(1) @Max(50) size: Int,
    ): ResponseEntity<CommonResponse<NotificationListResponse>> {
        val result = notificationQueryService.getNotifications(userId = userId, cursor = cursor, size = size)

        return ResponseEntity
            .status(NotificationSuccessCode.NOTIFICATIONS_RETRIEVED.status)
            .body(
                CommonResponse.success(
                    NotificationSuccessCode.NOTIFICATIONS_RETRIEVED,
                    NotificationListResponse.from(result),
                ),
            )
    }

    @PutMapping("/{notificationId}/read")
    override fun markAsRead(
        @LoginUserId userId: Long,
        @PathVariable notificationId: Long,
    ): ResponseEntity<Void> {
        notificationService.markAsRead(notificationId = notificationId, userId = userId)

        return ResponseEntity.noContent().build()
    }
}
