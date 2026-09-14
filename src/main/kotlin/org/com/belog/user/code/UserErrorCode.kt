package org.com.belog.user.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-E001", "사용자를 찾을 수 없습니다."),
    UNSUPPORTED_PROFILE_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "USER-E002", "지원하지 않는 프로필 이미지 형식입니다."),
    INVALID_PROFILE_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "USER-E003", "프로필 이미지는 5MB 이하여야 합니다."),
}
