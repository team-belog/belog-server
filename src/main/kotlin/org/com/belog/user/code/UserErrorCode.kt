package org.com.belog.user.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class UserErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER-E001", "사용자를 찾을 수 없습니다."),
    NICKNAME_ALREADY_EXISTS(HttpStatus.CONFLICT, "USER-E002", "이미 사용 중인 닉네임입니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "USER-E003", "이미 온보딩을 완료했습니다."),
    INVALID_PROFILE_IMAGE_OBJECT_KEY(HttpStatus.BAD_REQUEST, "USER-E004", "프로필 이미지 object key가 올바르지 않습니다."),
}
