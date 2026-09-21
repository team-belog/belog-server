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
import org.com.belog.meeting.controller.dto.request.SubmitDatePollResponseRequest
import org.com.belog.meeting.controller.dto.response.MeetingDatePollResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Meeting Date Poll", description = "만남 후보 일정 응답 API")
interface MeetingDatePollSwagger {
    @Operation(
        summary = "후보 일정과 내 응답 상태 조회",
        description =
            "해당 만남의 참여자가 후보 일정과 자신의 응답 상태를 조회합니다. " +
                "만남 생성자는 응답 레코드 없이 모든 후보 일정에 참석 가능한 상태로 반환됩니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "후보 일정 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = DATE_POLL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "일정 조율 방식의 만남이 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_DATE_POLL_MEETING_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남의 참여자가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_MEETING_PARTICIPANT_EXAMPLE)],
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
    fun getDatePoll(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
    ): ResponseEntity<CommonResponse<MeetingDatePollResponse>>

    @Operation(
        summary = "내 가능한 후보 일정 응답",
        description =
            "만남 생성자를 제외한 참여자가 참석 가능한 후보 일정을 한 번만 제출합니다. " +
                "빈 배열은 모든 후보 일정에 참석할 수 없다는 응답이며, 제출 후에는 변경할 수 없습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "후보 일정 응답 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = DATE_POLL_RESPONSE_SUBMITTED_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패 또는 유효하지 않은 후보 일정",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "중복 후보 일정", value = DUPLICATE_AVAILABLE_DATE_EXAMPLE),
                            ExampleObject(name = "다른 만남의 후보 일정", value = INVALID_AVAILABLE_DATE_EXAMPLE),
                            ExampleObject(name = "일정 조율 방식이 아님", value = NOT_DATE_POLL_MEETING_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "참여자가 아니거나 만남 생성자임",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "참여자가 아님", value = NOT_MEETING_PARTICIPANT_EXAMPLE),
                            ExampleObject(name = "생성자 응답", value = CREATOR_CANNOT_RESPOND_EXAMPLE),
                        ],
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
            ApiResponse(
                responseCode = "409",
                description = "일정 조율 중이 아니거나 이미 응답함",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "일정 조율 종료", value = DATE_POLL_NOT_SCHEDULING_EXAMPLE),
                            ExampleObject(name = "응답 완료", value = DATE_POLL_ALREADY_RESPONDED_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun submitMyDatePollResponse(
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
                    schema = Schema(implementation = SubmitDatePollResponseRequest::class),
                    examples = [
                        ExampleObject(name = "가능한 일정 선택", value = DATE_POLL_RESPONSE_REQUEST_EXAMPLE),
                        ExampleObject(name = "모든 일정 불가", value = NO_AVAILABLE_DATE_REQUEST_EXAMPLE),
                    ],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: SubmitDatePollResponseRequest,
    ): ResponseEntity<CommonResponse<Nothing>>
}

private const val DATE_POLL_RESPONSE_REQUEST_EXAMPLE =
    """{"candidateDateRangeIds":[101,103]}"""

private const val NO_AVAILABLE_DATE_REQUEST_EXAMPLE =
    """{"candidateDateRangeIds":[]}"""

private const val DATE_POLL_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S002","message":"후보 일정과 응답 상태를 조회했습니다.","data":{"meetingId":7,"status":"SCHEDULING","candidateDateRanges":[{"id":101,"startDate":"2026-10-03","endDate":"2026-10-04"},{"id":103,"startDate":"2026-10-10","endDate":"2026-10-11"}],"myResponse":{"responded":true,"respondedAt":"2026-09-22T00:00:00Z","selectedCandidateDateRangeIds":[101,103]}}}"""

private const val DATE_POLL_RESPONSE_SUBMITTED_EXAMPLE =
    """{"code":"MEETING-S003","message":"가능한 후보 일정 응답을 완료했습니다.","data":null}"""

private const val NOT_DATE_POLL_MEETING_EXAMPLE =
    """{"code":"MEETING-E012","message":"일정 조율 방식의 만남이 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val NOT_MEETING_PARTICIPANT_EXAMPLE =
    """{"code":"MEETING-E011","message":"해당 만남의 참여자가 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val CREATOR_CANNOT_RESPOND_EXAMPLE =
    """{"code":"MEETING-E013","message":"만남 생성자는 후보 일정에 응답할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val DATE_POLL_NOT_SCHEDULING_EXAMPLE =
    """{"code":"MEETING-E014","message":"일정 조율 중인 만남에만 응답할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val DATE_POLL_ALREADY_RESPONDED_EXAMPLE =
    """{"code":"MEETING-E015","message":"후보 일정 응답을 이미 완료했습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val DUPLICATE_AVAILABLE_DATE_EXAMPLE =
    """{"code":"MEETING-E016","message":"중복된 후보 일정이 선택되었습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val INVALID_AVAILABLE_DATE_EXAMPLE =
    """{"code":"MEETING-E017","message":"해당 만남의 후보 일정이 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""
