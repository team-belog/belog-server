package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogPhotoDayResult
import org.com.belog.postlog.service.result.PostLogPhotoListItemResult
import org.com.belog.postlog.service.result.PostLogPhotoListResult
import java.time.LocalDate
import java.time.OffsetDateTime

@Schema(description = "Post-log 사진 목록 조회 결과")
data class PostLogPhotoListResponse(
    @field:Schema(description = "일차 오름차순 사진 목록. 같은 일차의 사진이 다음 페이지에 이어질 수 있음")
    val days: List<PostLogPhotoDayResponse>,
    @field:Schema(description = "다음 페이지 커서. 다음 페이지가 없으면 null", nullable = true)
    val nextCursor: String?,
    @field:Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
) {
    companion object {
        fun from(result: PostLogPhotoListResult): PostLogPhotoListResponse =
            PostLogPhotoListResponse(
                days = result.days.map(PostLogPhotoDayResponse::from),
                nextCursor = result.nextCursor,
                hasNext = result.hasNext,
            )
    }
}

@Schema(description = "Post-log 일차별 사진 목록")
data class PostLogPhotoDayResponse(
    @field:Schema(description = "만남 시작일 기준 일차. 시작일 이전은 음수", example = "1")
    val dayNumber: Int,
    @field:Schema(description = "사진 촬영 현지 날짜", example = "2026-09-28")
    val date: LocalDate,
    @field:Schema(description = "촬영 시각과 사진 ID 오름차순 사진 목록")
    val photos: List<PostLogPhotoListItemResponse>,
) {
    companion object {
        fun from(result: PostLogPhotoDayResult): PostLogPhotoDayResponse =
            PostLogPhotoDayResponse(
                dayNumber = result.dayNumber,
                date = result.date,
                photos = result.photos.map(PostLogPhotoListItemResponse::from),
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
