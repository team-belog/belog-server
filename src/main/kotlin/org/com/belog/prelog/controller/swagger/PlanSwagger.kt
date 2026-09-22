package org.com.belog.prelog.controller.swagger

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
import org.com.belog.prelog.controller.dto.request.CreatePlanRequest
import org.com.belog.prelog.controller.dto.response.CreatePlanResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Pre-log", description = "만남 전 계획 관련 API")
interface PlanSwagger {
    @Operation(
        summary = "Pre-log 계획 생성",
        description =
            "만남 참여자가 링크 또는 메모 계획을 생성합니다. " +
                "LINK는 url만, MEMO는 content만 입력할 수 있으며 종료된 만남에는 계획을 추가할 수 없습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "계획 생성 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(name = "링크 계획 생성", value = CREATE_LINK_PLAN_SUCCESS_EXAMPLE),
                            ExampleObject(name = "메모 계획 생성", value = CREATE_MEMO_PLAN_SUCCESS_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 또는 계획 정보 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 계획 정보", value = INVALID_PLAN_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "만남 참여자가 아님",
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
            ApiResponse(
                responseCode = "409",
                description = "종료된 만남",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = MEETING_ALREADY_ENDED_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun createPlan(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "1", required = true)
        @PathVariable
        meetingId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = CreatePlanRequest::class),
                    examples = [
                        ExampleObject(name = "링크 계획", value = CREATE_LINK_PLAN_REQUEST_EXAMPLE),
                        ExampleObject(name = "메모 계획", value = CREATE_MEMO_PLAN_REQUEST_EXAMPLE),
                    ],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: CreatePlanRequest,
    ): ResponseEntity<CommonResponse<CreatePlanResponse>>
}

private const val CREATE_LINK_PLAN_REQUEST_EXAMPLE =
    """{"type":"LINK","category":"RESTAURANT","title":"광주 맛집","url":"https://example.com/place"}"""

private const val CREATE_MEMO_PLAN_REQUEST_EXAMPLE =
    """{"type":"MEMO","category":"RESTAURANT","title":"시드니 핫플 식당","content":"웨이팅을 대비해 여유롭게 일정을 잡아야 함"}"""

private const val CREATE_LINK_PLAN_SUCCESS_EXAMPLE =
    """{"code":"PRE_LOG-S001","message":"계획이 생성되었습니다.","data":{"planId":1,"meetingId":1,"creatorParticipantId":10,"type":"LINK","category":"RESTAURANT","title":"광주 맛집","url":"https://example.com/place","content":null}}"""

private const val CREATE_MEMO_PLAN_SUCCESS_EXAMPLE =
    """{"code":"PRE_LOG-S001","message":"계획이 생성되었습니다.","data":{"planId":2,"meetingId":1,"creatorParticipantId":10,"type":"MEMO","category":"RESTAURANT","title":"시드니 핫플 식당","url":null,"content":"웨이팅을 대비해 여유롭게 일정을 잡아야 함"}}"""

private const val INVALID_PLAN_EXAMPLE =
    """{"code":"PRE_LOG-E001","message":"계획 정보가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val NOT_MEETING_PARTICIPANT_EXAMPLE =
    """{"code":"MEETING-E011","message":"해당 만남의 참여자가 아닙니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""

private const val MEETING_ALREADY_ENDED_EXAMPLE =
    """{"code":"PRE_LOG-E002","message":"종료된 만남에는 계획을 추가할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-22T00:00:00Z"}}"""
