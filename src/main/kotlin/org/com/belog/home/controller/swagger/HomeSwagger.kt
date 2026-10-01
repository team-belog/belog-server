package org.com.belog.home.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.home.controller.dto.response.HomeCalendarResponse
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestParam
import java.time.YearMonth

@Tag(name = "Home", description = "홈 화면 관련 API")
interface HomeSwagger {
    @Operation(
        summary = "홈 월간 달력 조회",
        description =
            "로그인 사용자가 참여한 확정 만남 중 조회 월과 일정이 하루 이상 겹치는 만남을 조회합니다. " +
                "삭제된 만남과 일정 조율 중인 만남은 제외합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "홈 월간 달력 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = HOME_CALENDAR_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "조회 연월 형식 오류",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun getCalendar(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "조회 연월", example = "2026-08", required = true)
        @RequestParam
        @DateTimeFormat(pattern = "yyyy-MM")
        yearMonth: YearMonth,
    ): ResponseEntity<CommonResponse<HomeCalendarResponse>>

    companion object {
        private const val HOME_CALENDAR_SUCCESS_EXAMPLE =
            """{"code":"HOME-S001","message":"홈 달력을 조회했습니다.","data":{"yearMonth":"2026-08","today":"2026-08-18","meetings":[{"meetingId":11,"startDate":"2026-08-17","endDate":"2026-08-18"}]}}"""
    }
}
