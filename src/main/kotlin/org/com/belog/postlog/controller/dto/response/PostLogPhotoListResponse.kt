package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogPhotoListItemResult
import org.com.belog.postlog.service.result.PostLogPhotoListResult
import java.time.OffsetDateTime

@Schema(description = "Post-log 사진 목록 조회 결과")
data class PostLogPhotoListResponse(
    @field:Schema(description = "사진 목록")
    val items: List<PostLogPhotoListItemResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: PostLogPhotoListResult): PostLogPhotoListResponse =
            PostLogPhotoListResponse(
                items = result.items.map(PostLogPhotoListItemResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "Post-log 사진 목록 항목")
data class PostLogPhotoListItemResponse(
    @field:Schema(description = "사진 ID", example = "31")
    val photoId: Long,
    @field:Schema(description = "S3 Presigned GET URL")
    val photoUrl: String,
    @field:Schema(description = "UTC 오프셋을 포함한 사진 촬영 시각", example = "2026-09-28T12:00:00+09:00")
    val capturedAt: OffsetDateTime,
    @field:Schema(description = "전체 좋아요 수", example = "3")
    val likeCount: Long,
    @field:Schema(description = "로그인 사용자의 좋아요 여부", example = "true")
    val likedByMe: Boolean,
) {
    companion object {
        fun from(result: PostLogPhotoListItemResult): PostLogPhotoListItemResponse =
            PostLogPhotoListItemResponse(
                photoId = result.photoId,
                photoUrl = result.photoUrl,
                capturedAt = result.capturedAt,
                likeCount = result.likeCount,
                likedByMe = result.likedByMe,
            )
    }
}
