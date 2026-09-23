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
import org.com.belog.billlog.controller.dto.request.RegisterBillRequest
import org.com.belog.billlog.controller.dto.response.RegisterBillResponse
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Bill-log", description = "Bill-log 결제 내역 및 정산 요청 관련 API")
interface BillSwagger {
    @Operation(
        summary = "결제 내역 및 정산 요청 등록",
        description =
            "해당 만남이 속한 그룹의 멤버가 결제 내역을 등록합니다. " +
                "항목 합계와 개인별 부담 금액 합계는 결제 총액과 일치해야 하며, " +
                "결제자를 제외한 부담자에게 PENDING 정산 요청을 생성합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "결제 내역 및 정산 요청 등록 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = REGISTER_BILL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값, 결제 참여자 또는 금액 합계 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "항목 합계 불일치", value = ITEM_TOTAL_MISMATCH_EXAMPLE),
                            ExampleObject(name = "개인별 부담 금액 합계 불일치", value = SHARE_TOTAL_MISMATCH_EXAMPLE),
                            ExampleObject(name = "결제자 참여 권한 없음", value = INVALID_PAYER_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남이 속한 그룹의 멤버가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_GROUP_MEMBER_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "만남을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = MEETING_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun registerBill(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = RegisterBillRequest::class),
                    examples = [ExampleObject(value = REGISTER_BILL_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: RegisterBillRequest,
    ): ResponseEntity<CommonResponse<RegisterBillResponse>>
}

private const val REGISTER_BILL_REQUEST_EXAMPLE =
    """{"title":"아랑이 카페","payerMemberId":10,"totalAmount":11000,"splitType":"EQUAL_SPLIT","items":[{"name":"아메리카노","amount":4000},{"name":"프라푸치노","amount":7000}],"shares":[{"participantMemberId":10,"amount":5500},{"participantMemberId":11,"amount":5500}]}"""

private const val REGISTER_BILL_SUCCESS_EXAMPLE =
    """{"code":"BILL_LOG-S001","message":"결제 내역과 정산 요청이 등록되었습니다.","data":{"billId":1}}"""

private const val ITEM_TOTAL_MISMATCH_EXAMPLE =
    """{"code":"BILL_LOG-E005","message":"결제 항목 합계가 결제 총액과 일치하지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-23T00:00:00Z"}}"""

private const val SHARE_TOTAL_MISMATCH_EXAMPLE =
    """{"code":"BILL_LOG-E006","message":"개인별 부담 금액 합계가 결제 총액과 일치하지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-23T00:00:00Z"}}"""

private const val INVALID_PAYER_EXAMPLE =
    """{"code":"BILL_LOG-E008","message":"결제자는 해당 만남의 참여자여야 합니다.","data":{"fieldErrors":[],"timestamp":"2026-09-23T00:00:00Z"}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-23T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-23T00:00:00Z"}}"""
