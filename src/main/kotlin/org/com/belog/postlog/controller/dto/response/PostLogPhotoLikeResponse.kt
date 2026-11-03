package org.com.belog.postlog.controller.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.postlog.service.result.PostLogPhotoLikeResult

@Schema(description = "Post-log 사진 좋아요 상태 변경 결과")
data class PostLogPhotoLikeResponse(
    @field:Schema(description = "사진 ID", example = "31")
    val photoId: Long,
    @field:Schema(description = "로그인 사용자의 좋아요 여부", example = "true")
    val likedByMe: Boolean,
    @field:Schema(description = "전체 좋아요 수", example = "3")
    val likeCount: Long,
) {
    companion object {
        fun from(result: PostLogPhotoLikeResult): PostLogPhotoLikeResponse =
            PostLogPhotoLikeResponse(
                photoId = result.photoId,
                likedByMe = result.likedByMe,
                likeCount = result.likeCount,
            )
    }
}
