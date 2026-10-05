package org.com.belog.billlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.com.belog.billlog.domain.ReceiptImageFormat

data class ReceiptImageUploadUrlRequest(
    @field:Schema(description = "영수증 이미지 Content-Type", example = "image/jpeg")
    @field:NotBlank(message = "영수증 이미지 Content-Type을 입력해 주세요.")
    val contentType: String,
    @field:Schema(description = "영수증 이미지 크기(byte)", example = "2457600")
    @field:Positive(message = "파일 크기는 0보다 커야 합니다.")
    @field:Max(
        value = ReceiptImageFormat.MAX_FILE_SIZE_BYTES,
        message = "영수증 이미지는 10MB 이하여야 합니다.",
    )
    val fileSize: Long,
)
