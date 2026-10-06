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
import org.com.belog.billlog.controller.dto.request.ReceiptAnalysisRequest
import org.com.belog.billlog.controller.dto.request.ReceiptImageUploadUrlRequest
import org.com.belog.billlog.controller.dto.response.BillLogSummaryResponse
import org.com.belog.billlog.controller.dto.response.ReceiptAnalysisResponse
import org.com.belog.billlog.controller.dto.response.ReceiptImageUploadUrlResponse
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Bill-log", description = "Bill-log 결제 내역 및 정산 요청 관련 API")
interface BillLogSwagger {
    @Operation(
        summary = "Bill-log 요약 조회",
        description =
            "해당 만남의 전체 결제 금액과 참여자 단위의 정산 완료 및 미완료 인원을 조회합니다. " +
                "정산 요청이 한 건 이상 있는 참여자만 인원 집계에 포함합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Bill-log 요약 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_BILL_LOG_SUMMARY_SUCCESS_EXAMPLE)],
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
    fun getSummary(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
    ): ResponseEntity<CommonResponse<BillLogSummaryResponse>>

    @Operation(
        summary = "영수증 이미지 업로드 URL 발급",
        description =
            "해당 만남이 속한 그룹의 멤버에게 영수증 이미지의 S3 Presigned PUT URL을 발급합니다. " +
                "지원 형식은 JPEG와 PNG이며 최대 크기는 10MB입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "영수증 이미지 업로드 URL 발급 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = ISSUE_RECEIPT_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "이미지 형식 또는 크기 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "지원하지 않는 이미지 형식", value = UNSUPPORTED_RECEIPT_IMAGE_TYPE_EXAMPLE),
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
    fun issueReceiptImageUploadUrl(
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
                    schema = Schema(implementation = ReceiptImageUploadUrlRequest::class),
                    examples = [ExampleObject(value = ISSUE_RECEIPT_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: ReceiptImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<ReceiptImageUploadUrlResponse>>

    @Operation(
        summary = "영수증 이미지 분석",
        description =
            "그룹 멤버가 직접 업로드한 영수증 이미지를 분석하여 수정 가능한 결제 정보를 반환합니다. " +
                "이 단계에서는 Bill을 저장하지 않습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "영수증 이미지 분석 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = ANALYZE_RECEIPT_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "객체 키 또는 영수증 이미지 메타데이터 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 객체 키", value = INVALID_RECEIPT_IMAGE_OBJECT_KEY_EXAMPLE),
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
                description = "만남 또는 영수증 이미지를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "만남 없음", value = MEETING_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "영수증 이미지 없음", value = RECEIPT_IMAGE_NOT_FOUND_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "502",
                description = "Upstage 분석 실패 또는 비정상 응답",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = RECEIPT_ANALYSIS_FAILED_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "504",
                description = "Upstage 분석 시간 초과",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = RECEIPT_ANALYSIS_TIMEOUT_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun analyzeReceipt(
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
                    schema = Schema(implementation = ReceiptAnalysisRequest::class),
                    examples = [ExampleObject(value = ANALYZE_RECEIPT_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: ReceiptAnalysisRequest,
    ): ResponseEntity<CommonResponse<ReceiptAnalysisResponse>>
}

private const val GET_BILL_LOG_SUMMARY_SUCCESS_EXAMPLE =
    """{"code":"BILL_LOG-S003","message":"Bill-log 요약 정보를 조회했습니다.","data":{"totalSpentAmount":11000,"completedParticipantCount":1,"pendingParticipantCount":2}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-30T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-30T00:00:00Z"}}"""

private const val ISSUE_RECEIPT_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE =
    """{"contentType":"image/jpeg","fileSize":2457600}"""

private const val ISSUE_RECEIPT_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE =
    """{"code":"BILL_LOG-S006","message":"영수증 이미지 업로드 URL이 발급되었습니다.","data":{"objectKey":"bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg","uploadUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg?...","method":"PUT","requiredHeaders":{"Content-Type":"image/jpeg","Content-Length":"2457600"},"expiresAt":"2026-10-06T03:15:00Z"}}"""

private const val UNSUPPORTED_RECEIPT_IMAGE_TYPE_EXAMPLE =
    """{"code":"BILL_LOG-E016","message":"지원하지 않는 영수증 이미지 형식입니다.","data":{"fieldErrors":[],"timestamp":"2026-10-06T03:10:00Z"}}"""

private const val ANALYZE_RECEIPT_REQUEST_EXAMPLE =
    """{"receiptImageObjectKey":"bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg"}"""

private const val ANALYZE_RECEIPT_SUCCESS_EXAMPLE =
    """{"code":"BILL_LOG-S007","message":"영수증 이미지를 분석했습니다.","data":{"title":"벨로그 식당","paymentDate":"2026-10-06","items":[{"name":"파스타","amount":15000},{"name":"샐러드","amount":14000}],"totalAmount":29000,"requiresConfirmation":false}}"""

private const val INVALID_RECEIPT_IMAGE_OBJECT_KEY_EXAMPLE =
    """{"code":"BILL_LOG-E018","message":"영수증 이미지 경로가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-10-06T03:10:00Z"}}"""

private const val RECEIPT_IMAGE_NOT_FOUND_EXAMPLE =
    """{"code":"BILL_LOG-E019","message":"영수증 이미지를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-10-06T03:10:00Z"}}"""

private const val RECEIPT_ANALYSIS_FAILED_EXAMPLE =
    """{"code":"BILL_LOG-E021","message":"영수증 이미지 분석에 실패했습니다.","data":{"fieldErrors":[],"timestamp":"2026-10-06T03:10:00Z"}}"""

private const val RECEIPT_ANALYSIS_TIMEOUT_EXAMPLE =
    """{"code":"BILL_LOG-E022","message":"영수증 이미지 분석 시간이 초과되었습니다.","data":{"fieldErrors":[],"timestamp":"2026-10-06T03:10:00Z"}}"""
