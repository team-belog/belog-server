package org.com.belog.group.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.controller.dto.JoinGroupRequest
import org.com.belog.group.controller.dto.JoinGroupResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Group Membership", description = "그룹 참여 관련 API")
interface GroupMembershipSwagger {
    @Operation(
        summary = "초대 코드로 그룹 참여",
        description = "인증된 사용자를 초대 코드에 해당하는 그룹의 MEMBER로 등록합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "그룹 참여 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = JOIN_GROUP_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패 또는 잘못된 초대 코드",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 초대 코드", value = INVALID_INVITE_CODE_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "온보딩 미완료",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = ONBOARDING_REQUIRED_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "404",
                description = "사용자 또는 그룹을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "사용자 없음", value = USER_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "그룹 없음", value = GROUP_NOT_FOUND_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "409",
                description = "중복 참여 또는 그룹 정원 초과",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "중복 참여", value = ALREADY_GROUP_MEMBER_EXAMPLE),
                            ExampleObject(name = "정원 초과", value = GROUP_MEMBER_LIMIT_EXCEEDED_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun joinGroup(
        @Parameter(hidden = true)
        authentication: Authentication,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = JoinGroupRequest::class),
                    examples = [ExampleObject(value = JOIN_GROUP_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: JoinGroupRequest,
    ): ResponseEntity<CommonResponse<JoinGroupResponse>>
}

private const val JOIN_GROUP_REQUEST_EXAMPLE =
    """{"inviteCode":"AB12CD"}"""

private const val JOIN_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S003","message":"그룹에 참여했습니다.","data":{"groupId":1,"name":"주말 러닝 모임","currentMemberCount":8}}"""

private const val INVALID_INVITE_CODE_EXAMPLE =
    """{"code":"GROUP-E008","message":"초대 코드가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val ONBOARDING_REQUIRED_EXAMPLE =
    """{"code":"GROUP-E001","message":"온보딩을 완료한 사용자만 그룹 기능을 이용할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val USER_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GROUP_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E009","message":"초대 코드에 해당하는 그룹을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val ALREADY_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E010","message":"이미 참여한 그룹입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GROUP_MEMBER_LIMIT_EXCEEDED_EXAMPLE =
    """{"code":"GROUP-E011","message":"그룹 최대 인원에 도달했습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""
