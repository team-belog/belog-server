package org.com.belog.postlog.service.result

import java.time.LocalDate
import java.time.OffsetDateTime

data class PostLogPhotoListResult(
    val days: List<PostLogPhotoDayResult>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

data class PostLogPhotoDayResult(
    val dayNumber: Int,
    val date: LocalDate,
    val photos: List<PostLogPhotoListItemResult>,
)

data class PostLogPhotoListItemResult(
    val photoId: Long,
    val photoUrl: String,
    val capturedAt: OffsetDateTime,
    val likeCount: Long,
    val likedByMe: Boolean,
)
