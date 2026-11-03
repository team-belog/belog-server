package org.com.belog.group.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import org.com.belog.group.domain.GroupCoverImageFormat

data class GroupCoverImageUploadUrlRequest(
    @field:Schema(description = "업로드할 그룹 커버 이미지의 Content-Type", example = "image/webp")
    @field:NotBlank(message = "이미지 Content-Type을 입력해 주세요.")
    val contentType: String,
    @field:Schema(description = "업로드할 그룹 커버 이미지 크기(byte)", example = "524288")
    @field:Positive(message = "파일 크기는 0보다 커야 합니다.")
    @field:Max(
        value = GroupCoverImageFormat.MAX_FILE_SIZE_BYTES,
        message = "그룹 커버 이미지는 5MB 이하여야 합니다.",
    )
    val fileSize: Long,
)
