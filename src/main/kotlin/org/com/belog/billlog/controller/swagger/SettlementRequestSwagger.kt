package org.com.belog.billlog.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Bill-log", description = "Bill-log 결제 내역 및 정산 요청 관련 API")
interface SettlementRequestSwagger {
    @Operation(
        summary = "정산 요청 완료 처리",
        description =
            "정산 요청 대상자가 송금을 완료한 뒤 자신의 요청을 COMPLETED 상태로 변경합니다. " +
                "이미 완료된 요청은 최초 완료 시각을 유지하며 동일하게 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "정산 요청 완료 처리 성공"),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 정산 요청의 대상자가 아님",
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
    fun complete(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "정산 요청 ID", example = "12", required = true)
        @PathVariable
        settlementRequestId: Long,
    ): ResponseEntity<Void>
}

private const val ACCESS_DENIED_EXAMPLE =
    """{"code":"BILL_LOG-E014","message":"정산 요청 대상자만 완료할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val NOT_FOUND_EXAMPLE =
    """{"code":"BILL_LOG-E013","message":"정산 요청을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""
