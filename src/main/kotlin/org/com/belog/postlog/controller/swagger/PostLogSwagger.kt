package org.com.belog.postlog.controller.swagger

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
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.postlog.controller.dto.request.PostLogPhotoUploadUrlsRequest
import org.com.belog.postlog.controller.dto.request.RegisterPostLogPhotosRequest
import org.com.belog.postlog.controller.dto.response.PostLogPhotoLikeResponse
import org.com.belog.postlog.controller.dto.response.PostLogPhotoListResponse
import org.com.belog.postlog.controller.dto.response.PostLogPhotoUploadUrlsResponse
import org.com.belog.postlog.controller.dto.response.PostLogSummaryResponse
import org.com.belog.postlog.controller.dto.response.RegisterPostLogPhotosResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Post-log", description = "Post-log 관련 API")
interface PostLogSwagger {
    @Operation(
        summary = "Post-log 만남 정리 조회",
        description =
            "해당 만남이 속한 그룹의 멤버가 만남 정보, 추억 문구, 정산 요약과 참여 멤버를 조회합니다. " +
                "Post-log가 아직 생성되지 않은 경우에도 조회에 성공하며 추억 문구와 티켓 생성 시각은 null입니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "만남 정리 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = GET_POST_LOG_SUMMARY_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
        ],
    )
    fun getSummary(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
    ): ResponseEntity<CommonResponse<PostLogSummaryResponse>>

    @Operation(
        summary = "Post-log 사진 목록 조회",
        description =
            "해당 만남이 속한 그룹의 멤버가 등록된 사진을 촬영 시각과 사진 ID 오름차순으로 조회합니다. " +
                "각 사진의 Presigned GET URL, 좋아요 수와 로그인 사용자의 좋아요 여부를 반환합니다. " +
                "다음 페이지 조회에는 이전 응답의 nextCursor를 그대로 사용합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사진 목록 조회 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(name = "사진 목록", value = GET_PHOTOS_SUCCESS_EXAMPLE),
                            ExampleObject(name = "빈 사진 목록", value = GET_EMPTY_PHOTOS_SUCCESS_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "조회 개수 또는 커서 검증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "입력값 오류", ref = CommonOpenApiExample.INVALID_INPUT),
                            ExampleObject(name = "커서 오류", value = INVALID_PHOTO_CURSOR_EXAMPLE),
                        ],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
        ],
    )
    fun getPhotos(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "이전 응답의 다음 페이지 커서. 첫 요청에서는 생략")
        @RequestParam(required = false)
        cursor: String?,
        @Parameter(description = "조회 개수. 기본 및 최대 50개", example = "50")
        @RequestParam(defaultValue = "50")
        @Min(1)
        @Max(50)
        size: Int,
    ): ResponseEntity<CommonResponse<PostLogPhotoListResponse>>

    @Operation(
        summary = "Post-log 사진 업로드 URL 일괄 발급",
        description =
            "해당 만남이 속한 그룹의 멤버에게 최대 100개 사진의 S3 Presigned PUT URL을 발급합니다. " +
                "지원 형식은 JPEG, PNG, WebP이며 사진 한 장당 최대 크기는 5MB입니다. " +
                "clientPhotoId는 클라이언트가 생성하는 임시 식별자로, 서버는 저장하지 않고 응답에 그대로 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사진 업로드 URL 발급 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = ISSUE_PHOTO_UPLOAD_URLS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "사진 개수, 형식 또는 크기 검증 실패",
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
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
        ],
    )
    fun issuePhotoUploadUrls(
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
                    schema = Schema(implementation = PostLogPhotoUploadUrlsRequest::class),
                    examples = [ExampleObject(value = ISSUE_PHOTO_UPLOAD_URLS_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: PostLogPhotoUploadUrlsRequest,
    ): ResponseEntity<CommonResponse<PostLogPhotoUploadUrlsResponse>>

    @Operation(
        summary = "Post-log 사진 메타데이터 일괄 등록",
        description =
            "S3 업로드가 완료된 최대 100개 사진의 object key와 촬영 시각을 등록합니다. " +
                "Post-log가 없으면 자동으로 생성하며, 동일한 메타데이터의 재요청은 기존 등록 결과를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "사진 메타데이터 등록 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = REGISTER_PHOTOS_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "요청값, object key 또는 S3 사진 정보 검증 실패",
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
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
            ApiResponse(
                responseCode = "409",
                description = "동일한 object key가 다른 메타데이터로 이미 등록됨",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = PHOTO_OBJECT_KEY_CONFLICT_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "503",
                description = "S3 객체 검증 제한 시간 초과 또는 검증 요청 과부하",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = PHOTO_VERIFICATION_UNAVAILABLE_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    fun registerPhotos(
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
                    schema = Schema(implementation = RegisterPostLogPhotosRequest::class),
                    examples = [ExampleObject(value = REGISTER_PHOTOS_REQUEST_EXAMPLE)],
                ),
            ],
        )
        @Valid
        @RequestBody
        request: RegisterPostLogPhotosRequest,
    ): ResponseEntity<CommonResponse<RegisterPostLogPhotosResponse>>

    @Operation(
        summary = "Post-log 사진 좋아요 등록",
        description =
            "해당 만남이 속한 그룹의 멤버가 사진에 좋아요를 등록합니다. " +
                "이미 좋아요가 등록된 경우에도 성공하며 현재 좋아요 상태와 전체 좋아요 수를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사진 좋아요 등록 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = LIKE_PHOTO_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
                description = "만남 또는 해당 만남의 사진을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "만남 없음", value = MEETING_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "사진 없음", value = POST_LOG_PHOTO_NOT_FOUND_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun likePhoto(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "사진 ID", example = "31", required = true)
        @PathVariable
        photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>>

    @Operation(
        summary = "Post-log 사진 좋아요 취소",
        description =
            "해당 만남이 속한 그룹의 멤버가 자신이 등록한 사진 좋아요를 취소합니다. " +
                "등록된 좋아요가 없어도 성공하며 현재 좋아요 상태와 전체 좋아요 수를 반환합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사진 좋아요 취소 성공",
                useReturnTypeSchema = true,
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = UNLIKE_PHOTO_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(responseCode = "401", ref = CommonOpenApiResponse.AUTHENTICATION_REQUIRED),
            ApiResponse(
                responseCode = "403",
                description = "해당 만남이 속한 그룹의 멤버가 아님",
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
                description = "만남 또는 해당 만남의 사진을 찾을 수 없음",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(name = "만남 없음", value = MEETING_NOT_FOUND_EXAMPLE),
                            ExampleObject(name = "사진 없음", value = POST_LOG_PHOTO_NOT_FOUND_EXAMPLE),
                        ],
                    ),
                ],
            ),
        ],
    )
    fun unlikePhoto(
        @Parameter(hidden = true)
        @LoginUserId
        userId: Long,
        @Parameter(description = "만남 ID", example = "7", required = true)
        @PathVariable
        meetingId: Long,
        @Parameter(description = "사진 ID", example = "31", required = true)
        @PathVariable
        photoId: Long,
    ): ResponseEntity<CommonResponse<PostLogPhotoLikeResponse>>
}

private const val ISSUE_PHOTO_UPLOAD_URLS_REQUEST_EXAMPLE =
    """{"photos":[{"clientPhotoId":"photo-1","contentType":"image/jpeg","fileSize":2457600},{"clientPhotoId":"photo-2","contentType":"image/webp","fileSize":1843200}]}"""

private const val GET_POST_LOG_SUMMARY_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S006","message":"Post-log 만남 정리 정보가 조회되었습니다.","data":{"meetingId":7,"meetingName":"1박 2일 광주 여행","startDate":"2026-08-17","endDate":"2026-08-18","location":"대한민국 광주","memory":null,"settlement":{"completedParticipantCount":1,"totalAmount":11000},"participantCount":3,"participants":[{"groupMemberId":21,"nickname":"이정원","meetingCreator":true},{"groupMemberId":22,"nickname":"정다빈","meetingCreator":false},{"groupMemberId":23,"nickname":"김성연","meetingCreator":false}],"ticketCreated":false}}"""

private const val ISSUE_PHOTO_UPLOAD_URLS_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S001","message":"사진 업로드 URL이 발급되었습니다.","data":{"uploads":[{"clientPhotoId":"photo-1","objectKey":"post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg","uploadUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg?...","method":"PUT","requiredHeaders":{"Content-Type":"image/jpeg","Content-Length":"2457600"},"expiresAt":"2026-09-28T03:15:00Z"}]}}"""

private const val REGISTER_PHOTOS_REQUEST_EXAMPLE =
    """{"photos":[{"objectKey":"post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg","capturedAt":"2026-09-28T14:37:21+09:00"}]}"""

private const val REGISTER_PHOTOS_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S002","message":"사진 메타데이터가 등록되었습니다.","data":{"photos":[{"photoId":31,"objectKey":"post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg","capturedAt":"2026-09-28T14:37:21+09:00"}]}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":null}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":null}"""

private const val PHOTO_OBJECT_KEY_CONFLICT_EXAMPLE =
    """{"code":"POST_LOG-E008","message":"이미 다른 정보로 등록된 사진입니다.","data":null}"""

private const val PHOTO_VERIFICATION_UNAVAILABLE_EXAMPLE =
    """{"code":"POST_LOG-E009","message":"업로드된 사진을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요.","data":null}"""

private const val LIKE_PHOTO_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S003","message":"사진에 좋아요를 등록했습니다.","data":{"photoId":31,"likedByMe":true,"likeCount":3}}"""

private const val UNLIKE_PHOTO_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S004","message":"사진 좋아요를 취소했습니다.","data":{"photoId":31,"likedByMe":false,"likeCount":2}}"""

private const val POST_LOG_PHOTO_NOT_FOUND_EXAMPLE =
    """{"code":"POST_LOG-E010","message":"사진을 찾을 수 없습니다.","data":null}"""

private const val GET_PHOTOS_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S005","message":"사진 목록이 조회되었습니다.","data":{"items":[{"photoId":31,"photoUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/post-logs/7/photos/photo-1.jpg?...","capturedAt":"2026-09-28T12:00:00+09:00","likeCount":3,"likedByMe":true}],"nextCursor":"MjAyNi0wOS0yOFQwMzowMDowMFp8MzE","hasNext":true}}"""

private const val GET_EMPTY_PHOTOS_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S005","message":"사진 목록이 조회되었습니다.","data":{"items":[],"nextCursor":null,"hasNext":false}}"""

private const val INVALID_PHOTO_CURSOR_EXAMPLE =
    """{"code":"POST_LOG-E011","message":"사진 목록 커서가 올바르지 않습니다.","data":null}"""
