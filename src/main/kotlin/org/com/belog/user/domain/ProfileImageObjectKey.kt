package org.com.belog.user.domain

@JvmInline
value class ProfileImageObjectKey private constructor(
    val value: String,
) {
    companion object {
        const val MAX_LENGTH = 1024

        fun create(
            userId: Long,
            value: String,
        ): ProfileImageObjectKey {
            require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
            require(value.isNotBlank()) { "프로필 이미지 object key는 비어 있을 수 없습니다." }
            require(value.length <= MAX_LENGTH) { "프로필 이미지 object key는 ${MAX_LENGTH}자를 초과할 수 없습니다." }
            require(value.none(Char::isWhitespace)) { "프로필 이미지 object key에는 공백을 포함할 수 없습니다." }

            val prefix = "users/$userId/profile/"
            require(value.startsWith(prefix)) { "현재 사용자의 프로필 이미지 경로가 아닙니다." }

            val relativeKey = value.removePrefix(prefix)
            require(relativeKey.isNotEmpty()) { "프로필 이미지 파일 경로가 필요합니다." }
            require(relativeKey.split('/').none { pathSegment -> pathSegment == "." || pathSegment == ".." }) {
                "프로필 이미지 object key에 허용되지 않은 경로가 포함되어 있습니다."
            }

            return ProfileImageObjectKey(value)
        }
    }
}
