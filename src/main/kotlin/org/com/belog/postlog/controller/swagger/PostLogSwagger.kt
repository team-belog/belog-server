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
import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.com.belog.postlog.controller.dto.request.PostLogPhotoUploadUrlsRequest
import org.com.belog.postlog.controller.dto.response.PostLogPhotoUploadUrlsResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "Post-log", description = "Post-log 관련 API")
interface PostLogSwagger {
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
}

private const val ISSUE_PHOTO_UPLOAD_URLS_REQUEST_EXAMPLE =
    """{"photos":[{"clientPhotoId":"photo-1","contentType":"image/jpeg","fileSize":2457600},{"clientPhotoId":"photo-2","contentType":"image/webp","fileSize":1843200}]}"""

private const val ISSUE_PHOTO_UPLOAD_URLS_SUCCESS_EXAMPLE =
    """{"code":"POST_LOG-S001","message":"사진 업로드 URL이 발급되었습니다.","data":{"uploads":[{"clientPhotoId":"photo-1","objectKey":"post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg","uploadUrl":"https://belog-storage.s3.ap-northeast-2.amazonaws.com/post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg?...","method":"PUT","requiredHeaders":{"Content-Type":"image/jpeg","Content-Length":"2457600"},"expiresAt":"2026-09-28T03:15:00Z"}]}}"""

private const val NOT_GROUP_MEMBER_EXAMPLE =
    """{"code":"GROUP-E013","message":"그룹 멤버만 접근할 수 있습니다.","data":null}"""

private const val MEETING_NOT_FOUND_EXAMPLE =
    """{"code":"MEETING-E010","message":"만남을 찾을 수 없습니다.","data":null}"""
