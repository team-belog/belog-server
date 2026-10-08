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
    UNSUPPORTED_PROFILE_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "USER-E005", "지원하지 않는 프로필 이미지 형식입니다."),
    INVALID_PROFILE_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "USER-E006", "프로필 이미지는 5MB 이하여야 합니다."),
    BANK_ACCOUNT_NOT_REGISTERED(HttpStatus.NOT_FOUND, "USER-E007", "등록된 계좌가 없습니다."),
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "USER-E008", "온보딩을 완료한 사용자만 이용할 수 있습니다."),
    PROFILE_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "USER-E009", "업로드된 프로필 이미지를 찾을 수 없습니다."),
    INVALID_PROFILE_IMAGE_METADATA(HttpStatus.BAD_REQUEST, "USER-E010", "프로필 이미지 정보가 올바르지 않습니다."),
    ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "USER-E011", "이미 탈퇴한 사용자입니다."),
}
