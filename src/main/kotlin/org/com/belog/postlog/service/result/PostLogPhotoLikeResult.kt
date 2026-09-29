package org.com.belog.postlog.service.result

data class PostLogPhotoLikeResult(
    val photoId: Long,
    val likedByMe: Boolean,
    val likeCount: Long,
)
