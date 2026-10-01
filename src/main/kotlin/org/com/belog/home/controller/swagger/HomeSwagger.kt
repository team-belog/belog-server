package org.com.belog.home.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.home.controller.dto.response.ActiveMeetingListResponse
import org.com.belog.home.controller.dto.response.CompletedMeetingListResponse
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

    @Operation(
        summary = "진행 중인 만남 목록 조회",
        description =
            "로그인 사용자가 참여한 일정 조율 중인 만남과 종료일이 오늘 이후인 확정 만남을 조회합니다. " +
                "만남명 또는 그룹명으로 검색할 수 있으며 시작일과 만남 ID 오름차순으로 정렬합니다. " +
                "일정 조율 중인 만남을 먼저 반환하며 날짜는 null, 진행 상태는 SCHEDULING입니다. " +
                "오늘은 Asia/Seoul 기준입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "진행 중인 만남 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = ACTIVE_MEETINGS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "커서 또는 조회 개수가 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(name = "조회 개수 오류", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 커서", value = INVALID_CURSOR_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun getActiveMeetings(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남명 또는 그룹명 검색어", example = "광주")
        @RequestParam(required = false)
        query: String?,
        @Parameter(description = "다음 페이지 커서. 첫 요청에서는 생략")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "조회 개수. 기본 10개, 최대 50개", example = "10")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<ActiveMeetingListResponse>>

    @Operation(
        summary = "종료된 만남 티켓 목록 조회",
        description =
            "로그인 사용자가 생성한 Post-log 티켓 중 종료일이 오늘보다 이전인 만남의 티켓을 조회합니다. " +
                "티켓이 없는 종료된 만남과 삭제된 만남은 제외하며 종료일과 Post-log ID 내림차순으로 정렬합니다. " +
                "오늘은 Asia/Seoul 기준입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "종료된 만남 티켓 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = COMPLETED_MEETINGS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "커서 또는 조회 개수가 올바르지 않음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(name = "조회 개수 오류", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 커서", value = INVALID_CURSOR_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun getCompletedMeetings(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "다음 페이지 커서. 첫 요청에서는 생략")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "조회 개수. 기본 10개, 최대 50개", example = "10")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<CompletedMeetingListResponse>>

    companion object {
        private const val HOME_CALENDAR_SUCCESS_EXAMPLE =
            """{"code":"HOME-S001","message":"홈 달력을 조회했습니다.","data":{"yearMonth":"2026-08","today":"2026-08-18","meetings":[{"meetingId":11,"startDate":"2026-08-17","endDate":"2026-08-18"}]}}"""

        private const val ACTIVE_MEETINGS_SUCCESS_EXAMPLE =
            """{"code":"HOME-S002","message":"진행 중인 만남 목록을 조회했습니다.","data":{"items":[{"meetingId":10,"name":"제주 여행","startDate":null,"endDate":null,"groupName":"피놀리와 기니휘기","progressStatus":"SCHEDULING","participantCount":3,"previewParticipants":[{"groupMemberId":21,"nickname":"이정원","profileImageUrl":null}]},{"meetingId":11,"name":"1박 2일 광주 여행","startDate":"2026-08-20","endDate":"2026-08-21","groupName":"피놀리와 기니휘기","progressStatus":"UPCOMING","participantCount":3,"previewParticipants":[]}],"nextCursor":null,"hasNext":false}}"""

        private const val COMPLETED_MEETINGS_SUCCESS_EXAMPLE =
            """{"code":"HOME-S003","message":"종료된 만남 목록을 조회했습니다.","data":{"items":[{"postLogId":31,"meetingId":7,"name":"1박 2일 광주 여행","memory":"친구들과 다녀온 첫 여행","coverPhotoUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/post-logs/7/photos/photo-1.jpg?...","startDate":"2026-07-17","endDate":"2026-07-18","location":"광주","participantCount":2,"participants":[{"groupMemberId":21,"nickname":"이정원"},{"groupMemberId":22,"nickname":"김민지"}]}],"nextCursor":null,"hasNext":false}}"""

        private const val INVALID_CURSOR_EXAMPLE =
            """{"code":"HOME-E001","message":"홈 만남 목록 커서가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-10-01T00:00:00Z"}}"""
    }
}
