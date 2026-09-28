package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogPhotoUploadResult
import java.time.Instant

data class PostLogPhotoUploadUrlsResponse(
    @field:Schema(description = "사진별 업로드 정보")
    val uploads: List<PostLogPhotoUploadUrlResponse>,
) {
    companion object {
        fun from(results: List<PostLogPhotoUploadResult>): PostLogPhotoUploadUrlsResponse =
            PostLogPhotoUploadUrlsResponse(
                uploads = results.map(PostLogPhotoUploadUrlResponse::from),
            )
    }
}

data class PostLogPhotoUploadUrlResponse(
    @field:Schema(
        description =
            "요청에서 전달받아 그대로 반환하는 클라이언트 임시 사진 식별자. " +
                "클라이언트는 이 값으로 원본 사진과 업로드 URL을 연결할 수 있습니다.",
        example = "photo-1",
    )
    val clientPhotoId: String,
    @field:Schema(description = "S3 객체 키", example = "post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg")
    val objectKey: String,
    @field:Schema(description = "S3 Presigned URL")
    val uploadUrl: String,
    @field:Schema(description = "S3 업로드 HTTP 메서드", example = "PUT")
    val method: String,
    @field:Schema(description = "S3 업로드 시 반드시 포함할 요청 헤더")
    val requiredHeaders: Map<String, String>,
    @field:Schema(description = "Presigned URL 만료 시각", example = "2026-09-28T03:15:00Z")
    val expiresAt: Instant,
) {
    companion object {
        fun from(result: PostLogPhotoUploadResult): PostLogPhotoUploadUrlResponse =
            PostLogPhotoUploadUrlResponse(
                clientPhotoId = result.clientPhotoId,
                objectKey = result.upload.objectKey,
                uploadUrl = result.upload.uploadUrl,
                method = "PUT",
                requiredHeaders =
                    mapOf(
                        "Content-Type" to result.upload.contentType,
                        "Content-Length" to result.upload.contentLength.toString(),
                    ),
                expiresAt = result.upload.expiresAt,
            )
    }
}
