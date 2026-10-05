package org.com.belog.billlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.billlog.domain.ReceiptImageUpload
import java.time.Instant

data class ReceiptImageUploadUrlResponse(
    @field:Schema(
        description = "영수증 이미지 S3 객체 키",
        example = "bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg",
    )
    val objectKey: String,
    @field:Schema(description = "S3 Presigned URL")
    val uploadUrl: String,
    @field:Schema(description = "S3 업로드 HTTP 메서드", example = "PUT")
    val method: String,
    @field:Schema(description = "S3 업로드 시 반드시 포함할 요청 헤더")
    val requiredHeaders: Map<String, String>,
    @field:Schema(description = "Presigned URL 만료 시각", example = "2026-10-06T03:15:00Z")
    val expiresAt: Instant,
) {
    companion object {
        fun from(upload: ReceiptImageUpload): ReceiptImageUploadUrlResponse =
            ReceiptImageUploadUrlResponse(
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
