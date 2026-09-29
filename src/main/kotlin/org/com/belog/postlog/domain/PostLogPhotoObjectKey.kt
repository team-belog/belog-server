package org.com.belog.postlog.domain

@JvmInline
value class PostLogPhotoObjectKey private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 512

        fun create(
            meetingId: Long,
            value: String,
        ): PostLogPhotoObjectKey {
            require(meetingId > 0) { "만남 식별자는 양수여야 합니다." }
            require(value.isNotBlank()) { "Post-log 사진 object key는 비어 있을 수 없습니다." }
            require(value.length <= MAX_LENGTH) {
                "Post-log 사진 object key는 ${MAX_LENGTH}자를 초과할 수 없습니다."
            }
            require(value.none(Char::isWhitespace)) { "Post-log 사진 object key에는 공백을 포함할 수 없습니다." }

            val prefix = "post-logs/$meetingId/photos/"
            require(value.startsWith(prefix)) { "해당 만남의 Post-log 사진 경로가 아닙니다." }

            val relativeKey = value.removePrefix(prefix)
            require(relativeKey.isNotEmpty()) { "Post-log 사진 파일 경로가 필요합니다." }
            require(relativeKey.split('/').none { pathSegment -> pathSegment == "." || pathSegment == ".." }) {
                "Post-log 사진 object key에 허용되지 않은 경로가 포함되어 있습니다."
            }

            return PostLogPhotoObjectKey(value)
        }
    }
}
