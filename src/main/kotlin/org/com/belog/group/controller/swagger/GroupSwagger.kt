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
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.Positive
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.group.controller.dto.request.CreateGroupRequest
import org.com.belog.group.controller.dto.request.GroupCoverImageUploadUrlRequest
import org.com.belog.group.controller.dto.request.UpdateGroupCoverImageRequest
import org.com.belog.group.controller.dto.response.CreateGroupResponse
import org.com.belog.group.controller.dto.response.GroupCoverImageUploadUrlResponse
import org.com.belog.group.controller.dto.response.GroupDetailResponse
import org.com.belog.group.controller.dto.response.GroupMembersResponse
import org.com.belog.group.controller.dto.response.MyGroupListResponse
import org.com.belog.group.controller.dto.response.PastMeetingListResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Group", description = "그룹 관련 API")
interface GroupSwagger {
    @Operation(
        summary = "내 그룹 목록 조회",
        description =
            "로그인 사용자가 참여 중인 그룹을 고정 그룹 우선, 최근 가입 순으로 조회합니다. " +
                "그룹별 전체 멤버 수와 OWNER 우선, 가입 순으로 정렬된 최대 3명의 멤버 미리보기를 제공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "내 그룹 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_MY_GROUPS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "커서 또는 조회 개수 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
        ],
    )
    fun getMyGroups(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "다음 페이지 커서. 첫 요청에서는 생략", example = "MTo0Mg")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "조회 개수. 기본 10개, 최대 50개", example = "10")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<MyGroupListResponse>>

    @Operation(
        summary = "그룹 고정",
        description =
            "로그인 사용자가 참여 중인 그룹을 고정합니다. " +
                "여러 그룹을 고정할 수 있으며 이미 고정된 그룹에 대한 요청도 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 고정 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = PIN_GROUP_SUCCESS_EXAMPLE)],
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
    fun pinGroup(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
    ): ResponseEntity<CommonResponse<Nothing>>

    @Operation(
        summary = "그룹 고정 해제",
        description =
            "로그인 사용자가 참여 중인 그룹의 고정을 해제합니다. " +
                "고정되지 않은 그룹에 대한 요청도 성공합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 고정 해제 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = UNPIN_GROUP_SUCCESS_EXAMPLE)],
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
    fun unpinGroup(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
    ): ResponseEntity<CommonResponse<Nothing>>

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
        summary = "지난 만남 목록 조회",
        description =
            "종료일이 현재 영업일보다 이전인 확정 만남을 최신 생성순으로 조회합니다. " +
                "만남 ID 기반 커서 페이지네이션을 사용하며 해당 그룹에 참여한 사용자만 조회할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "지난 만남 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_PAST_MEETINGS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "쿼리 파라미터 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(ref = CommonOpenApiExample.INVALID_INPUT)],
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
    fun getPastMeetings(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "그룹 ID", example = "1", required = true)
        @PathVariable
        groupId: Long,
        @Parameter(description = "마지막으로 조회한 지난 만남 ID. 첫 요청에서는 생략", example = "8")
        @RequestParam(required = false)
        @Positive
        cursor: Long?,
        @Parameter(description = "조회 개수. 기본 10개, 최대 50개", example = "10")
        @RequestParam(defaultValue = "10")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<PastMeetingListResponse>>

    @Operation(
        summary = "그룹 멤버 목록 조회",
        description =
            "그룹 멤버를 OWNER 우선, 가입 순으로 조회합니다. " +
                "query를 전달하면 이름 또는 닉네임이 검색어를 포함하는 멤버를 영문 대소문자 구분 없이 조회합니다. " +
                "query가 없거나 공백이면 전체 멤버를 조회하며, 해당 그룹에 참여한 사용자만 이용할 수 있습니다.",
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
        @Parameter(description = "이름 또는 닉네임 검색어. 영문 대소문자를 구분하지 않음", example = "정원")
        @RequestParam(required = false)
        query: String?,
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
        summary = "그룹 커버 이미지 변경",
        description =
            "미리 업로드한 이미지의 Object Key로 그룹 커버 이미지를 변경합니다. " +
                "해당 그룹의 OWNER만 변경할 수 있습니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "그룹 커버 이미지 변경 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = UPDATE_COVER_IMAGE_SUCCESS_EXAMPLE)],
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
                description = "그룹 멤버가 아니거나 OWNER 권한이 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "그룹 멤버가 아님", value = NOT_GROUP_MEMBER_EXAMPLE),
                            ExampleObject(name = "OWNER 권한 없음", value = GROUP_OWNER_REQUIRED_EXAMPLE),
                        ],
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
    fun updateCoverImage(
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
                    schema = Schema(implementation = UpdateGroupCoverImageRequest::class),
                    examples = [ExampleObject(value = UPDATE_COVER_IMAGE_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: UpdateGroupCoverImageRequest,
    ): ResponseEntity<CommonResponse<Nothing>>

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

private const val UPDATE_COVER_IMAGE_REQUEST_EXAMPLE =
    """{"coverImageObjectKey":"group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp"}"""

private const val UPDATE_COVER_IMAGE_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S006","message":"그룹 커버 이미지가 변경되었습니다.","data":null}"""

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

private const val GET_MY_GROUPS_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S007","message":"내 그룹 목록을 조회했습니다.","data":{"items":[{"groupId":1,"name":"피놀리와 기니휘기","coverImageUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/group-covers/15/image.webp?...","memberCount":3,"previewMembers":[{"groupMemberId":21,"nickname":"이정원","profileImageUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/users/15/profile/image.webp?..."},{"groupMemberId":22,"nickname":"정다빈","profileImageUrl":null},{"groupMemberId":23,"nickname":"김성연","profileImageUrl":"https://lh3.googleusercontent.com/profile"}],"pinned":true,"canDeleteGroup":true}],"nextCursor":"MTo0Mg","hasNext":true}}"""

private const val PIN_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S008","message":"그룹이 고정되었습니다.","data":null}"""

private const val UNPIN_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S009","message":"그룹 고정이 해제되었습니다.","data":null}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GROUP_OWNER_REQUIRED_EXAMPLE =
    """{"code":"GROUP-E014","message":"그룹 OWNER만 커버 이미지를 변경할 수 있습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GROUP_NOT_FOUND_EXAMPLE =
    """{"code":"GROUP-E012","message":"그룹을 찾을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-20T00:00:00Z"}}"""

private const val GET_GROUP_SUCCESS_EXAMPLE =
    """{"code":"GROUP-S005","message":"그룹을 조회했습니다.","data":{"groupId":1,"name":"피놀리와 기니휘기","coverImageUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/group-covers/15/image.webp?...","inviteCode":"QCRJNN","memberCount":15,"canEditCoverImage":true,"canDeleteGroup":true,"schedulingMeetings":[{"meetingId":10,"name":"1박 2일 광주 여행","participantNicknames":["이정원","정다빈","김성연"],"participantCount":3}],"activeMeetings":[{"meetingId":11,"name":"여름 부산 여행","startDate":"2026-09-25","endDate":"2026-09-26"}]}}"""

private const val GET_PAST_MEETINGS_SUCCESS_EXAMPLE =
    """{"code":"MEETING-S007","message":"지난 만남 목록을 조회했습니다.","data":{"items":[{"meetingId":7,"name":"2025 연말 파티","startDate":"2025-12-30","endDate":"2025-12-30"}],"nextCursor":7,"hasNext":true}}"""
