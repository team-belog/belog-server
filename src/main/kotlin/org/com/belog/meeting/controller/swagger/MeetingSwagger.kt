package org.com.belog.meeting.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.meeting.controller.dto.request.CreateMeetingRequest
import org.com.belog.meeting.controller.dto.response.CreateMeetingResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Meeting", description = "만남 관련 API")
interface MeetingSwagger {
    @Operation(
        summary = "만남 생성",
        description =
            "그룹 멤버가 만남을 생성합니다. schedule.type에 따라 날짜를 바로 확정하거나 후보 날짜 조율을 시작합니다.\n\n" +
                "- FIXED: 날짜를 바로 확정하는 방식\n" +
                "- POLL: 후보 날짜를 등록해 조율하는 방식\n\n" +
                "생성자는 참여자에 자동으로 포함되며, participantMemberIds에는 같은 그룹의 다른 멤버 ID만 전달할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "만남 생성 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = CREATE_MEETING_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값, 참여자 또는 확정 날짜 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "다른 그룹의 참여자", value = INVALID_PARTICIPANT_EXAMPLE),
                            ExampleObject(name = "중복 참여자", value = DUPLICATE_PARTICIPANT_EXAMPLE),
                            ExampleObject(name = "생성자를 참여자로 지정", value = CREATOR_INCLUDED_EXAMPLE),
                            ExampleObject(name = "과거 날짜", value = PAST_MEETING_DATE_EXAMPLE),
                            ExampleObject(name = "잘못된 일정 범위", value = INVALID_MEETING_DATE_RANGE_EXAMPLE),
                            ExampleObject(name = "지원하지 않는 일정 방식", value = UNSUPPORTED_SCHEDULE_TYPE_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "그룹 멤버가 아님",
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
                description = "그룹을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = GROUP_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun createMeeting(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = CreateMeetingRequest::class),
                    examples = [ExampleObject(value = CREATE_MEETING_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: CreateMeetingRequest,
    ): ResponseEntity<CommonResponse<CreateMeetingResponse>>
}

private const val CREATE_MEETING_REQUEST_EXAMPLE =
    """{"name":"광주 1박 2일","location":"서울고속버스터미널","participantMemberIds":[22,23],"schedule":{"type":"FIXED","startDate":"2026-10-03","endDate":"2026-10-04"}}"""

private const val CREATE_MEETING_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S001","message":"만남이 생성되었습니다.","data":{"meetingId":1,"groupId":1,"name":"광주 1박 2일","location":"서울고속버스터미널","scheduleType":"FIXED","status":"CONFIRMED","startDate":"2026-10-03","endDate":"2026-10-04","confirmedAt":"2026-09-21T00:00:00Z","participantCount":3}}"""

private const val INVALID_PARTICIPANT_EXAMPLE =
    """{"code":"MEETING-E001","message":"만남 참여자는 해당 그룹의 멤버여야 합니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val DUPLICATE_PARTICIPANT_EXAMPLE =
    """{"code":"MEETING-E002","message":"만남 참여자가 중복되었습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val CREATOR_INCLUDED_EXAMPLE =
    """{"code":"MEETING-E003","message":"만남 생성자는 참여자 목록에 포함할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val PAST_MEETING_DATE_EXAMPLE =
    """{"code":"MEETING-E005","message":"과거 날짜로 만남을 생성할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val INVALID_MEETING_DATE_RANGE_EXAMPLE =
    """{"code":"MEETING-E007","message":"만남 종료일은 시작일보다 빠를 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val UNSUPPORTED_SCHEDULE_TYPE_EXAMPLE =
    """{"code":"MEETING-E006","message":"지원하지 않는 일정 등록 방식입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"해당 그룹의 멤버가 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val GROUP_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E012","message":"그룹을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""
