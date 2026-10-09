package org.com.belog.notification.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.notification.controller.dto.request.RegisterNotificationDeviceRequest
import org.com.belog.notification.controller.dto.request.UpdatePushNotificationSettingRequest
import org.com.belog.notification.controller.dto.response.NotificationListResponse
import org.com.belog.notification.controller.dto.response.PushNotificationSettingResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Notification", description = "알림 관련 API")
interface NotificationSwagger {
    @Operation(
        summary = "푸시 알림 수신 설정 조회",
        description = "로그인 사용자의 푸시 알림 수신 여부를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "푸시 알림 수신 설정 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = PUSH_NOTIFICATION_SETTING_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun getPushNotificationSetting(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
    ): ResponseEntity<CommonResponse<PushNotificationSettingResponse>>

    @Operation(
        summary = "푸시 알림 수신 설정 변경",
        description = "로그인 사용자의 푸시 알림 수신 여부를 변경합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "푸시 알림 수신 설정 변경 성공"),
            ApiResponse(
                responseCode = "400",
                description = "푸시 알림 수신 여부가 누락됨",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun updatePushNotificationSetting(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "변경할 푸시 알림 수신 여부",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UpdatePushNotificationSettingRequest::class),
                    examples = [ExampleObject(value = UPDATE_PUSH_NOTIFICATION_SETTING_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: UpdatePushNotificationSettingRequest,
    ): ResponseEntity<CommonResponse<Nothing>>

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

    @Operation(
        summary = "알림 읽음 처리",
        description =
            "로그인 사용자의 알림을 읽음 처리합니다. 이미 읽은 알림은 기존 읽은 시각을 유지하며 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "알림 읽음 처리 성공"),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "404",
                description = "알림이 없거나 로그인 사용자의 알림이 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOTIFICATION_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun markAsRead(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "읽음 처리할 알림 ID", example = "101", required = true)
        @PathVariable
        notificationId: Long,
    ): ResponseEntity<Void>

    @Operation(
        summary = "푸시 기기 등록 및 토큰 갱신",
        description =
            "로그인 사용자의 기기를 등록하고 FCM 토큰을 저장합니다. 이미 등록된 기기면 토큰을 갱신합니다. " +
                "동일한 FCM 토큰이 다른 기기나 사용자에 등록되어 있었다면 기존 등록은 비활성화됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "기기 등록 또는 토큰 갱신 성공"),
            ApiResponse(
                responseCode = "400",
                description = "FCM 토큰이 비어 있거나 형식이 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "404",
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun registerDevice(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "클라이언트가 생성한 기기 식별자", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
        @PathVariable
        deviceId: String,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            description = "등록할 FCM 토큰",
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = RegisterNotificationDeviceRequest::class),
                    examples = [ExampleObject(value = REGISTER_NOTIFICATION_DEVICE_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: RegisterNotificationDeviceRequest,
    ): ResponseEntity<CommonResponse<Nothing>>

    @Operation(
        summary = "푸시 기기 등록 해제",
        description = "로그인 사용자가 자신의 기기 등록을 해제합니다. 이미 해제된 기기도 성공으로 처리합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "기기 등록 해제 성공"),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "404",
                description = "등록된 기기가 없거나 로그인 사용자의 기기가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOTIFICATION_DEVICE_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun unregisterDevice(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "해제할 기기 식별자", example = "550e8400-e29b-41d4-a716-446655440000", required = true)
        @PathVariable
        deviceId: String,
    ): ResponseEntity<Void>

    companion object {
        private const val PUSH_NOTIFICATION_SETTING_SUCCESS_EXAMPLE =
            """{"code":"NOTIFICATION-S002","message":"푸시 알림 수신 설정을 조회했습니다.","data":{"pushNotificationEnabled":true}}"""
        private const val UPDATE_PUSH_NOTIFICATION_SETTING_REQUEST_EXAMPLE =
            """{"pushNotificationEnabled":false}"""
        private const val USER_NOT_FOUND_EXAMPLE =
            """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":null}"""
        private const val NOTIFICATION_LIST_SUCCESS_EXAMPLE =
            """{"code":"NOTIFICATION-S001","message":"알림 목록을 조회했습니다.","data":{"items":[{"notificationId":101,"type":"SETTLEMENT_REQUESTED","message":"피놀 님이 정산을 요청했어요","target":{"type":"BILL_LOG","id":7},"read":false,"createdAt":"2026-10-01T04:30:00Z"}],"nextCursor":101,"hasNext":true}}"""
        private const val NOTIFICATION_NOT_FOUND_EXAMPLE =
            """{"code":"NOTIFICATION-E001","message":"알림을 찾을 수 없습니다.","data":null}"""
        private const val REGISTER_NOTIFICATION_DEVICE_REQUEST_EXAMPLE =
            """{"fcmToken":"fcm-token-example"}"""
        private const val NOTIFICATION_DEVICE_NOT_FOUND_EXAMPLE =
            """{"code":"NOTIFICATION-E002","message":"등록된 기기를 찾을 수 없습니다.","data":null}"""
    }
}
