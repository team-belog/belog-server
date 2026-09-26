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
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.controller.dto.request.CreateGroupRequest
import org.com.belog.group.controller.dto.request.GroupCoverImageUploadUrlRequest
import org.com.belog.group.controller.dto.response.CreateGroupResponse
import org.com.belog.group.controller.dto.response.GroupCoverImageUploadUrlResponse
import org.com.belog.group.controller.dto.response.GroupDetailResponse
import org.com.belog.group.controller.dto.response.GroupMembersResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Group", description = "그룹 관련 API")
interface GroupSwagger {
    @Operation(
        summary = "그룹 조회",
        description =
            "그룹 정보와 일정 조율 중인 만남, 종료되지 않은 확정 만남을 조회합니다. " +
                "해당 그룹에 참여한 사용자만 조회할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_GROUP_SUCCESS_EXAMPLE)],
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
    fun getGroup(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
    ): ResponseEntity<CommonResponse<GroupDetailResponse>>

    @Operation(
        summary = "그룹 멤버 목록 조회",
        description = "그룹 멤버를 OWNER 우선, 가입 순으로 조회합니다. 해당 그룹에 참여한 사용자만 조회할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 멤버 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_GROUP_MEMBERS_SUCCESS_EXAMPLE)],
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
    fun getGroupMembers(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
    ): ResponseEntity<CommonResponse<GroupMembersResponse>>

    @Operation(
        summary = "그룹 커버 이미지 업로드 URL 발급",
        description = "JPEG, PNG 또는 WebP 그룹 커버 이미지를 S3에 직접 업로드할 수 있는 Presigned URL을 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 커버 이미지 업로드 URL 발급 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = COVER_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값 검증 실패 또는 지원하지 않는 이미지",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "요청값 검증 실패", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "지원하지 않는 이미지 형식", value = UNSUPPORTED_COVER_IMAGE_TYPE_EXAMPLE),
                            ExampleObject(name = "이미지 크기 초과", value = INVALID_COVER_IMAGE_SIZE_EXAMPLE),
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
        ],
    )
    fun issueCoverImageUploadUrl(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [
                Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = Schema(implementation = GroupCoverImageUploadUrlRequest::class),
                    examples = [ExampleObject(value = COVER_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: GroupCoverImageUploadUrlRequest,
    ): ResponseEntity<CommonResponse<GroupCoverImageUploadUrlResponse>>

    @Operation(
        summary = "그룹 생성",
        description =
            "그룹을 생성하고 요청한 사용자를 최초 멤버이자 OWNER로 등록한 뒤 초대 정보를 발급합니다. " +
                "커버 이미지는 업로드 URL API로 S3에 업로드한 후 반환된 objectKey를 전달합니다.",
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
                            ExampleObject(name = "업로드되지 않은 커버 이미지", value = COVER_IMAGE_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "잘못된 커버 이미지 정보", value = INVALID_COVER_IMAGE_METADATA_EXAMPLE),
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
        @LoginUserId
        userId: Long,
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

private const val COVER_IMAGE_UPLOAD_URL_REQUEST_EXAMPLE =
    """{"contentType":"image/webp","fileSize":524288}"""

private const val COVER_IMAGE_UPLOAD_URL_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S002","message":"그룹 커버 이미지 업로드 URL이 발급되었습니다.","data":{"objectKey":"group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp","uploadUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp?...","method":"PUT","requiredHeaders":{"Content-Type":"image/webp","Content-Length":"524288"},"expiresAt":"2026-09-17T03:05:00Z"}}"""

private const val CREATE_GROUP_REQUEST_EXAMPLE =
    """{"name":"주말 러닝 모임","coverImageObjectKey":"group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp"}"""

private const val CREATE_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S001","message":"그룹이 생성되었습니다.","data":{"groupId":1,"name":"주말 러닝 모임","currentMemberCount":1,"inviteCode":"AB12CD","inviteLink":"https://belog.co.kr/invitations/AB12CD"}}"""

private const val INVALID_COVER_IMAGE_OBJECT_KEY_EXAMPLE =
    """{"code":"GROUP-E003","message":"그룹 커버 이미지 object key가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val COVER_IMAGE_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E006","message":"업로드된 그룹 커버 이미지를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-17T00:00:00Z"}}"""

private const val INVALID_COVER_IMAGE_METADATA_EXAMPLE =
    """{"code":"GROUP-E007","message":"그룹 커버 이미지 정보가 올바르지 않습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-17T00:00:00Z"}}"""

private const val UNSUPPORTED_COVER_IMAGE_TYPE_EXAMPLE =
    """{"code":"GROUP-E004","message":"지원하지 않는 그룹 커버 이미지 형식입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-17T00:00:00Z"}}"""

private const val INVALID_COVER_IMAGE_SIZE_EXAMPLE =
    """{"code":"GROUP-E005","message":"그룹 커버 이미지는 5MB 이하여야 합니다.","data":{"fieldErrors":[],"timestamp":"2026-09-17T00:00:00Z"}}"""

private const val ONBOARDING_REQUIRED_EXAMPLE =
    """{"code":"GROUP-E001","message":"온보딩을 완료한 사용자만 그룹 기능을 이용할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val USER_NOT_FOUND_EXAMPLE =
    """{"code":"USER-E001","message":"사용자를 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val INVITE_CODE_ISSUANCE_FAILED_EXAMPLE =
    """{"code":"GROUP-E002","message":"초대 코드를 발급할 수 없습니다. 잠시 후 다시 시도해 주세요.","data":{"fieldErrors":[],"timestamp":"2026-09-15T00:00:00Z"}}"""

private const val GET_GROUP_MEMBERS_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S004","message":"그룹 멤버 목록을 조회했습니다.","data":{"items":[{"groupMemberId":21,"nickname":"방장","profileImageUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp?...","role":"OWNER"},{"groupMemberId":22,"nickname":"멤버","profileImageUrl":"https://lh3.googleusercontent.com/profile","role":"MEMBER"}]}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GROUP_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E012","message":"그룹을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GET_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S005","message":"그룹을 조회했습니다.","data":{"groupId":1,"name":"피놀리와 기니휘기","coverImageUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/group-covers/15/image.webp?...","inviteCode":"QCRJNN","memberCount":15,"canEditCoverImage":true,"canDeleteGroup":true,"schedulingMeetings":[{"meetingId":10,"name":"1박 2일 광주 여행","participantNicknames":["이정원","정다빈","김성연"],"participantCount":3}],"activeMeetings":[{"meetingId":11,"name":"여름 부산 여행","startDate":"2026-09-25","endDate":"2026-09-26"}]}}"""
