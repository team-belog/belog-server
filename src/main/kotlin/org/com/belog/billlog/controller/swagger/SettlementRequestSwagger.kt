package org.com.belog.billlog.controller.swagger

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
import org.com.belog.billlog.controller.dto.response.SettlementRequestListResponse
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Bill-log", description = "Bill-log 결제 내역 및 정산 요청 관련 API")
interface SettlementRequestSwagger {
    @Operation(
        summary = "정산 현황 조회",
        description =
            "해당 만남의 모든 정산 요청을 조회합니다. PENDING 요청을 COMPLETED 요청보다 먼저 배치하고, " +
                "각 상태 안에서는 최신순으로 정렬합니다. 진행 중인 요청에서 로그인 사용자가 수취인이면 " +
                "SEND_REMINDER, 송금자이면 MARK_COMPLETE, 그 외에는 NONE 액션을 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "정산 현황 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_SETTLEMENT_REQUESTS_SUCCESS_EXAMPLE)],
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
                responseCode = "400",
                description = "정산 현황 커서가 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INVALID_CURSOR_EXAMPLE)],
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
    fun getSettlementRequests(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "이전 응답의 다음 페이지 커서. 첫 요청에서는 생략", example = "UEVORElORzoxMDE")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "조회 개수. 기본 20개, 최대 50개", example = "20")
        @RequestParam(defaultValue = "20")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<SettlementRequestListResponse>>

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

private const val GET_SETTLEMENT_REQUESTS_SUCCESS_EXAMPLE =
    """{"code":"BILL_LOG-S004","message":"정산 현황을 조회했습니다.","data":{"items":[{"settlementRequestId":101,"amount":7500,"sender":{"meetingParticipantId":32,"nickname":"바비","profileImageUrl":"https://example.com/profile.webp","isMe":false},"receiver":{"meetingParticipantId":31,"nickname":"정바미","profileImageUrl":null,"isMe":true},"status":"PENDING","action":"SEND_REMINDER"},{"settlementRequestId":90,"amount":12000,"sender":{"meetingParticipantId":33,"nickname":"아랑","profileImageUrl":null,"isMe":false},"receiver":{"meetingParticipantId":32,"nickname":"바비","profileImageUrl":"https://example.com/profile.webp","isMe":false},"status":"COMPLETED","action":"NONE"}],"nextCursor":null,"hasNext":false}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-30T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-30T00:00:00Z"}}"""

private const val INVALID_CURSOR_EXAMPLE =
    """{"code":"BILL_LOG-E015","message":"정산 현황 커서가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-30T00:00:00Z"}}"""

private const val ACCESS_DENIED_EXAMPLE =
    """{"code":"BILL_LOG-E014","message":"정산 요청 대상자만 완료할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""

private const val NOT_FOUND_EXAMPLE =
    """{"code":"BILL_LOG-E013","message":"정산 요청을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-28T00:00:00Z"}}"""
