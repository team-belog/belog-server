package org.com.belog.group.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class GroupErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "GROUP-E001", "온보딩을 완료한 사용자만 그룹을 생성할 수 있습니다."),
    INVITE_CODE_ISSUANCE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "GROUP-E002", "초대 코드를 발급할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_COVER_IMAGE_OBJECT_KEY(HttpStatus.BAD_REQUEST, "GROUP-E003", "그룹 커버 이미지 object key가 올바르지 않습니다."),
}
