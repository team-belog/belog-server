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
import org.com.belog.meeting.controller.dto.request.UpdateMeetingRequest
import org.com.belog.meeting.controller.dto.response.CreateMeetingResponse
import org.com.belog.meeting.controller.dto.response.MeetingDetailResponse
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
                        examples = [
                            ExampleObject(
                                name = "확정 일정 만남 생성",
                                value = CREATE_FIXED_MEETING_SUCCESS_EXAMPLE,
                            ),
                            ExampleObject(
                                name = "일정 조율 만남 생성",
                                value = CREATE_POLL_MEETING_SUCCESS_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값, 참여자 또는 일정 범위 검증 실패",
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
                            ExampleObject(name = "중복 후보 일정", value = DUPLICATE_CANDIDATE_DATE_RANGE_EXAMPLE),
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
                    examples = [
                        ExampleObject(
                            name = "확정 일정 만남",
                            value = CREATE_FIXED_MEETING_REQUEST_EXAMPLE,
                        ),
                        ExampleObject(
                            name = "일정 조율 만남",
                            value = CREATE_POLL_MEETING_REQUEST_EXAMPLE,
                        ),
                    ],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: CreateMeetingRequest,
    ): ResponseEntity<CommonResponse<CreateMeetingResponse>>

    @Operation(
        summary = "만남 수정",
        description =
            "만남 생성자가 만남명, 장소와 확정된 일정을 수정합니다. " +
                "일정 조율 중인 만남은 startDate와 endDate에 null을 전달해야 하며, " +
                "종료된 만남은 일정은 유지한 채 만남명과 장소만 수정할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "만남 수정 성공",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = UPDATE_MEETING_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 또는 일정 범위 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 일정 범위", value = INVALID_MEETING_DATE_RANGE_EXAMPLE),
                            ExampleObject(name = "확정 일정 누락", value = MEETING_DATE_REQUIRED_EXAMPLE),
                            ExampleObject(name = "과거 일정", value = PAST_MEETING_DATE_SELECTION_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "만남 생성자가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_MEETING_CREATOR_EXAMPLE)],
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
                description = "만남 상태상 일정 변경 불가",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "종료된 만남", value = MEETING_ALREADY_ENDED_EXAMPLE),
                            ExampleObject(name = "일정 조율 중인 만남", value = MEETING_DATE_NOT_CONFIRMED_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun updateMeeting(
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
                    schema = Schema(implementation = UpdateMeetingRequest::class),
                    examples = [ExampleObject(value = UPDATE_MEETING_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: UpdateMeetingRequest,
    ): ResponseEntity<CommonResponse<Nothing>>

    @Operation(
        summary = "만남 삭제",
        description =
            "만남 생성자가 만남을 삭제합니다. 삭제 시각을 기록하는 soft delete 방식이며, " +
                "연관 데이터와 S3 객체는 보존됩니다. 이미 삭제된 만남은 찾을 수 없는 만남으로 처리합니다. " +
                "완료되지 않은 정산 요청이 있으면 삭제할 수 없으며, 이미 생성된 Post-log 티켓은 삭제 후에도 조회할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "만남 삭제 성공",
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "만남 생성자가 아님",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = NOT_MEETING_CREATOR_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "만남을 찾을 수 없거나 이미 삭제됨",
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
                description = "완료되지 않은 정산 요청이 있음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = UNSETTLED_SETTLEMENT_REQUEST_EXISTS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun deleteMeeting(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
    ): ResponseEntity<CommonResponse<Nothing>>

    @Operation(
        summary = "만남 상세 조회",
        description =
            "만남 기본 정보와 로그인 사용자의 수정 권한, " +
                "Pre-log, Bill-log, Post-log의 작성 상태를 조회합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "만남 상세 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = MEETING_DETAIL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 그룹의 멤버가 아님",
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
            ApiResponse(responseCode = "500", ref = CommonOpenApiResponse.INTERNAL_SERVER_ERROR),
        ],
    )
    fun getMeetingDetail(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
    ): ResponseEntity<CommonResponse<MeetingDetailResponse>>
}

private const val CREATE_FIXED_MEETING_REQUEST_EXAMPLE =
    """{"name":"광주 1박 2일","location":"서울고속버스터미널","participantMemberIds":[22,23],"schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"}]}}"""

private const val CREATE_POLL_MEETING_REQUEST_EXAMPLE =
    """{"name":"가을 여행","location":"서울고속버스터미널","participantMemberIds":[22,23],"schedule":{"type":"POLL","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"},{"startDate":"2026-10-10","endDate":"2026-10-11"}]}}"""

private const val UPDATE_MEETING_REQUEST_EXAMPLE =
    """{"name":"1박 2일 광주 여행","location":"광주광역시 000 000","startDate":"2026-10-03","endDate":"2026-10-04"}"""

private const val CREATE_FIXED_MEETING_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S001","message":"만남이 생성되었습니다.","data":{"meetingId":1}}"""

private const val CREATE_POLL_MEETING_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S001","message":"만남이 생성되었습니다.","data":{"meetingId":2}}"""

private const val UPDATE_MEETING_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S008","message":"만남이 수정되었습니다.","data":null}"""

private const val MEETING_DETAIL_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S006","message":"만남 상세 정보를 조회했습니다.","data":{"meetingId":7,"groupId":1,"groupName":"피놀리와 기니휘기","meetingName":"1박 2일 광주 여행","scheduleType":"FIXED","meetingStatus":"CONFIRMED","startDate":"2026-10-03","endDate":"2026-10-04","location":"광주광역시 000 000","canEditMeeting":true,"logStatuses":{"preLog":"IN_PROGRESS","billLog":"NOT_STARTED","postLog":"NOT_STARTED"}}}"""

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

private const val DUPLICATE_CANDIDATE_DATE_RANGE_EXAMPLE =
    """{"code":"MEETING-E009","message":"중복된 후보 일정 범위가 포함되어 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"해당 그룹의 멤버가 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val GROUP_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E012","message":"그룹을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val NOT_MEETING_CREATOR_EXAMPLE =
    """{"code":"MEETING-E018","message":"만남 생성자만 만남을 관리할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val UNSETTLED_SETTLEMENT_REQUEST_EXISTS_EXAMPLE =
    """{"code":"BILL_LOG-E023","message":"아직 정산이 완료되지 않았어요","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val MEETING_ALREADY_ENDED_EXAMPLE =
    """{"code":"MEETING-E019","message":"종료된 만남의 일정은 변경할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val MEETING_DATE_NOT_CONFIRMED_EXAMPLE =
    """{"code":"MEETING-E021","message":"확정된 만남의 일정만 변경할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val PAST_MEETING_DATE_SELECTION_EXAMPLE =
    """{"code":"MEETING-E022","message":"과거 날짜를 만남 일정으로 지정할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""

private const val MEETING_DATE_REQUIRED_EXAMPLE =
    """{"code":"MEETING-E023","message":"확정된 만남의 시작일과 종료일은 필수입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-21T00:00:00Z"}}"""
