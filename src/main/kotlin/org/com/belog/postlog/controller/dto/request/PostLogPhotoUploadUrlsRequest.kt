package org.com.belog.postlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Positive
import jakarta.validation.constraints.Size
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoFormat
import org.com.belog.postlog.service.command.PostLogPhotoUploadTarget

data class PostLogPhotoUploadUrlsRequest(
    @field:Schema(description = "업로드할 사진 목록. 한 요청당 최대 100장")
    @field:Size(
        min = 1,
        max = PostLogPhoto.MAX_UPLOAD_COUNT,
        message = "사진은 1장 이상 100장 이하로 요청해 주세요.",
    )
    @field:Valid
    val photos: List<PostLogPhotoUploadUrlItemRequest>,
)

data class PostLogPhotoUploadUrlItemRequest(
    @field:Schema(
        description =
            "클라이언트가 사진별 업로드 상태를 관리하고 요청과 응답을 연결하기 위해 생성한 임시 식별자. " +
                "서버는 저장하지 않고 응답에 동일한 값을 반환합니다.",
        example = "photo-1",
    )
    @field:NotBlank(message = "클라이언트 사진 식별자를 입력해 주세요.")
    @field:Size(
        max = PostLogPhotoUploadTarget.CLIENT_PHOTO_ID_MAX_LENGTH,
        message = "클라이언트 사진 식별자는 100자를 초과할 수 없습니다.",
    )
    val clientPhotoId: String,
    @field:Schema(description = "사진 Content-Type", example = "image/jpeg")
    @field:NotBlank(message = "사진 Content-Type을 입력해 주세요.")
    val contentType: String,
    @field:Schema(description = "사진 크기(byte)", example = "2457600")
    @field:Positive(message = "파일 크기는 0보다 커야 합니다.")
    @field:Max(
        value = PostLogPhotoFormat.MAX_FILE_SIZE_BYTES,
        message = "사진은 5MB 이하여야 합니다.",
    )
    val fileSize: Long,
) {
    fun toTarget(): PostLogPhotoUploadTarget =
        PostLogPhotoUploadTarget(
            clientPhotoId = clientPhotoId,
            contentType = contentType,
            fileSize = fileSize,
        )
}
