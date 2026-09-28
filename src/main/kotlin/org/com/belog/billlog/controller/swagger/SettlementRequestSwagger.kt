package org.com.belog.billlog.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.com.belog.billlog.controller.dto.request.UpdateSettlementRequestStatusRequest
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Bill-log", description = "Bill-log 결제 내역 및 정산 요청 관련 API")
interface SettlementRequestSwagger {
    @Operation(
        summary = "정산 요청 완료 처리",
        description =
            "결제자가 입금을 확인한 정산 요청을 COMPLETED 상태로 변경합니다. " +
                "이미 완료된 요청은 최초 완료 시각을 유지하며 동일하게 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "정산 요청 완료 처리 성공"),
            ApiResponse(
                responseCode = "400",
                description = "요청값 또는 상태 전이 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 상태", value = INVALID_STATUS_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 결제 내역의 결제자가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = ACCESS_DENIED_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 만남에 속한 정산 요청을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun updateStatus(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "정산 요청 ID", example = "12", required = true)
        @PathVariable
        settlementRequestId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = UpdateSettlementRequestStatusRequest::class),
                    examples = [ExampleObject(value = UPDATE_STATUS_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: UpdateSettlementRequestStatusRequest,
    ): ResponseEntity<Void>
}

private const val UPDATE_STATUS_REQUEST_EXAMPLE = """{"status":"COMPLETED"}"""

private const val INVALID_STATUS_EXAMPLE =
    """{"code":"BILL_LOG-E015","message":"정산 요청은 완료 상태로만 변경할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val ACCESS_DENIED_EXAMPLE =
    """{"code":"BILL_LOG-E014","message":"결제자만 정산을 완료할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val NOT_FOUND_EXAMPLE =
    """{"code":"BILL_LOG-E013","message":"정산 요청을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""
