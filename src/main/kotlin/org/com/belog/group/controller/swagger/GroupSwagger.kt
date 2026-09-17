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
import org.com.belog.group.controller.dto.CreateGroupRequest
import org.com.belog.group.controller.dto.CreateGroupResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Group", description = "그룹 관련 API")
interface GroupSwagger {
    @Operation(
        summary = "그룹 생성",
        description = "그룹을 생성하고 요청한 사용자를 최초 멤버이자 OWNER로 등록한 뒤 초대 정보를 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "그룹 생성 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = CREATE_GROUP_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 또는 커버 이미지 Object Key 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "잘못된 커버 이미지 Object Key", value = INVALID_COVER_IMAGE_OBJECT_KEY_EXAMPLE),
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
                description = "사용자를 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = USER_NOT_FOUND_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "503",
                description = "초대 코드 발급 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INVITE_CODE_ISSUANCE_FAILED_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun createGroup(
        @Parameter(hidden = true)
        authentication: Authentication,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = CreateGroupRequest::class),
                    examples = [ExampleObject(value = CREATE_GROUP_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: CreateGroupRequest,
    ): ResponseEntity<CommonResponse<CreateGroupResponse>>
}

private const val CREATE_GROUP_REQUEST_EXAMPLE =
    """{"name":"주말 러닝 모임","coverImageObjectKey":"group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp"}"""

private const val CREATE_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S001","message":"그룹이 생성되었습니다.","data":{"groupId":1,"name":"주말 러닝 모임","currentMemberCount":1,"inviteCode":"AB12CD","inviteLink":"https://belog.co.kr/invitations/AB12CD"}}"""

private const val INVALID_COVER_IMAGE_OBJECT_KEY_EXAMPLE =
    """{"code":"GROUP-E003","message":"그룹 커버 이미지 object key가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val ONBOARDING_REQUIRED_EXAMPLE =
    """{"code":"GROUP-E001","message":"온보딩을 완료한 사용자만 그룹 기능을 이용할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val USER_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val INVITE_CODE_ISSUANCE_FAILED_EXAMPLE =
    """{"code":"GROUP-E002","message":"초대 코드를 발급할 수 없습니다. 잠시 후 다시 시도해 주세요.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""
