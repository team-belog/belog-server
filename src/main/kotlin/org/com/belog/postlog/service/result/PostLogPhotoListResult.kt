package org.com.belog.postlog.service.result

import java.time.OffsetDateTime

data class PostLogPhotoListResult(
    val items: List<PostLogPhotoListItemResult>,
    val nextCursor: String?,
    val hasNext: Boolean,
)

data class PostLogPhotoListItemResult(
    val photoId: Long,
    val photoUrl: String,
    val capturedAt: OffsetDateTime,
    val likeCount: Long,
    val likedByMe: Boolean,
)
