package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.domain.PostLogPhoto
import java.time.OffsetDateTime

data class RegisterPostLogPhotosResponse(
    @field:Schema(description = "등록된 사진 목록")
    val photos: List<RegisteredPostLogPhotoResponse>,
) {
    companion object {
        fun from(photos: List<PostLogPhoto>): RegisterPostLogPhotosResponse =
            RegisterPostLogPhotosResponse(
                photos = photos.map(RegisteredPostLogPhotoResponse::from),
            )
    }
}

data class RegisteredPostLogPhotoResponse(
    @field:Schema(description = "등록된 사진 ID", example = "31")
    val photoId: Long,
    @field:Schema(
        description = "S3 객체 키",
        example = "post-logs/7/photos/550e8400-e29b-41d4-a716-446655440000.jpg",
    )
    val objectKey: String,
    @field:Schema(description = "UTC 오프셋을 포함한 사진 촬영 시각", example = "2026-09-28T14:37:21+09:00")
    val capturedAt: OffsetDateTime,
) {
    companion object {
        fun from(photo: PostLogPhoto): RegisteredPostLogPhotoResponse =
            RegisteredPostLogPhotoResponse(
                photoId = checkNotNull(photo.id) { "등록된 Post-log 사진의 ID가 없습니다." },
                objectKey = photo.objectKey,
                capturedAt = photo.capturedAtWithOffset(),
            )
    }
}
