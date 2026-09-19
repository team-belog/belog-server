package org.com.belog.group.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class GroupErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    ONBOARDING_REQUIRED(HttpStatus.FORBIDDEN, "GROUP-E001", "온보딩을 완료한 사용자만 그룹 기능을 이용할 수 있습니다."),
    INVITE_CODE_ISSUANCE_FAILED(HttpStatus.SERVICE_UNAVAILABLE, "GROUP-E002", "초대 코드를 발급할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    INVALID_COVER_IMAGE_OBJECT_KEY(HttpStatus.BAD_REQUEST, "GROUP-E003", "그룹 커버 이미지 object key가 올바르지 않습니다."),
    UNSUPPORTED_COVER_IMAGE_TYPE(HttpStatus.BAD_REQUEST, "GROUP-E004", "지원하지 않는 그룹 커버 이미지 형식입니다."),
    INVALID_COVER_IMAGE_SIZE(HttpStatus.BAD_REQUEST, "GROUP-E005", "그룹 커버 이미지는 5MB 이하여야 합니다."),
    COVER_IMAGE_NOT_FOUND(HttpStatus.BAD_REQUEST, "GROUP-E006", "업로드된 그룹 커버 이미지를 찾을 수 없습니다."),
    INVALID_COVER_IMAGE_METADATA(HttpStatus.BAD_REQUEST, "GROUP-E007", "그룹 커버 이미지 정보가 올바르지 않습니다."),
    INVALID_INVITE_CODE(HttpStatus.BAD_REQUEST, "GROUP-E008", "초대 코드가 올바르지 않습니다."),
    GROUP_NOT_FOUND_BY_INVITE_CODE(HttpStatus.NOT_FOUND, "GROUP-E009", "초대 코드에 해당하는 그룹을 찾을 수 없습니다."),
    ALREADY_GROUP_MEMBER(HttpStatus.CONFLICT, "GROUP-E010", "이미 참여한 그룹입니다."),
    GROUP_MEMBER_LIMIT_EXCEEDED(HttpStatus.CONFLICT, "GROUP-E011", "그룹 최대 인원에 도달했습니다."),
}
