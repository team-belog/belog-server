package org.com.belog.notification.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.notification.controller.dto.response.NotificationListResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Notification", description = "알림 관련 API")
interface NotificationSwagger {
    @Operation(
        summary = "알림 목록 조회",
        description =
            "로그인 사용자의 알림을 최신순으로 조회합니다. " +
                "notificationId 기반 커서 페이지네이션을 사용하며 cursor가 없으면 첫 페이지를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "알림 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = NOTIFICATION_LIST_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "커서 또는 조회 개수가 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
        ],
    )
    fun getNotifications(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "마지막으로 조회한 알림 ID", example = "101")
        @RequestParam(required = false)
        @Positive
        cursor: Long?,
        @Parameter(description = "조회할 알림 개수", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<NotificationListResponse>>

    companion object {
        private const val NOTIFICATION_LIST_SUCCESS_EXAMPLE =
            """{"code":"NOTIFICATION-S001","message":"알림 목록을 조회했습니다.","data":{"items":[{"notificationId":101,"type":"SETTLEMENT_REQUESTED","message":"피놀 님이 정산을 요청했어요","targetId":7,"read":false,"createdAt":"2026-10-01T04:30:00Z"}],"nextCursor":101,"hasNext":true}}"""
    }
}
