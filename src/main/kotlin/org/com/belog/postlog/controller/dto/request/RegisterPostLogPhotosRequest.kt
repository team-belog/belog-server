package org.com.belog.postlog.controller.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.service.command.PostLogPhotoRegistrationTarget
import java.time.OffsetDateTime

data class RegisterPostLogPhotosRequest(
    @field:Schema(description = "등록할 사진 메타데이터 목록. 한 요청당 최대 100장")
    @field:Size(
        min = 1,
        max = PostLogPhoto.MAX_UPLOAD_COUNT,
        message = "사진은 1장 이상 100장 이하로 요청해 주세요.",
    )
    @field:Valid
    val photos: List<RegisterPostLogPhotoItemRequest>,
)

data class RegisterPostLogPhotoItemRequest(
    @field:Schema(
        description = "사진 업로드 URL 발급 API에서 반환한 S3 객체 키",
        example = "post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg",
    )
    @field:NotBlank(message = "사진 object key를 입력해 주세요.")
    @field:Size(
        max = PostLogPhotoObjectKey.MAX_LENGTH,
        message = "사진 object key는 512자를 초과할 수 없습니다.",
    )
    val objectKey: String,
    @field:Schema(
        description = "UTC 오프셋을 포함한 사진 촬영 시각",
        example = "2026-09-28T14:37:21+09:00",
    )
    val capturedAt: OffsetDateTime,
) {
    fun toTarget(): PostLogPhotoRegistrationTarget =
        PostLogPhotoRegistrationTarget(
            objectKey = objectKey,
            capturedAt = capturedAt,
        )
}
