package org.com.belog.user.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.user.domain.ProfileImageUpload
import java.time.Instant

data class ProfileImageUploadUrlResponse(
    @field:Schema(description = "S3 객체 키", example = "users/15/profile/550e8400-e29b-41d4-a716-446655440000.webp")
    val objectKey: String,
    @field:Schema(description = "S3 Presigned URL")
    val uploadUrl: String,
    @field:Schema(description = "업로드 HTTP 메서드", example = "PUT")
    val method: String,
    @field:Schema(description = "S3 업로드 시 반드시 포함할 요청 헤더")
    val requiredHeaders: Map<String, String>,
    @field:Schema(description = "Presigned URL 만료 시각", example = "2026-09-14T14:05:00Z")
    val expiresAt: Instant,
) {
    companion object {
        fun from(upload: ProfileImageUpload): ProfileImageUploadUrlResponse =
            ProfileImageUploadUrlResponse(
                objectKey = upload.objectKey,
                uploadUrl = upload.uploadUrl,
                method = "PUT",
                requiredHeaders =
                    mapOf(
                        "Content-Type" to upload.contentType,
                        "Content-Length" to upload.contentLength.toString(),
                    ),
                expiresAt = upload.expiresAt,
            )
    }
}
