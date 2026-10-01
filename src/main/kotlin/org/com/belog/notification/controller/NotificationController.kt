package org.com.belog.notification.controller

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.response.CommonResponse
import org.com.belog.notification.code.NotificationSuccessCode
import org.com.belog.notification.controller.dto.response.NotificationListResponse
import org.com.belog.notification.controller.swagger.NotificationSwagger
import org.com.belog.notification.service.NotificationQueryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
class NotificationController(
    private val notificationQueryService: NotificationQueryService,
) : NotificationSwagger {
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
}
