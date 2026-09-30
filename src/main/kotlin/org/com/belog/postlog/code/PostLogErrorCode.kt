package org.com.belog.postlog.code

import org.com.belog.global.response.code.ErrorCode
import org.springframework.http.HttpStatus

enum class PostLogErrorCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : ErrorCode {
    INVALID_PHOTO_UPLOAD_REQUEST(HttpStatus.BAD_REQUEST, "POST_LOG-E001", "사진 업로드 요청이 올바르지 않습니다."),
    UNSUPPORTED_PHOTO_TYPE(HttpStatus.BAD_REQUEST, "POST_LOG-E002", "지원하지 않는 사진 형식입니다."),
    INVALID_PHOTO_SIZE(HttpStatus.BAD_REQUEST, "POST_LOG-E003", "사진은 5MB 이하여야 합니다."),
    PHOTO_NOT_FOUND(HttpStatus.BAD_REQUEST, "POST_LOG-E004", "업로드된 사진을 찾을 수 없습니다."),
    INVALID_PHOTO_METADATA(HttpStatus.BAD_REQUEST, "POST_LOG-E005", "업로드된 사진 정보가 올바르지 않습니다."),
    INVALID_PHOTO_OBJECT_KEY(HttpStatus.BAD_REQUEST, "POST_LOG-E006", "사진 object key가 올바르지 않습니다."),
    INVALID_CAPTURED_AT(HttpStatus.BAD_REQUEST, "POST_LOG-E007", "사진 촬영 시각이 올바르지 않습니다."),
    PHOTO_OBJECT_KEY_CONFLICT(HttpStatus.CONFLICT, "POST_LOG-E008", "이미 다른 정보로 등록된 사진입니다."),
    PHOTO_VERIFICATION_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "POST_LOG-E009", "업로드된 사진을 확인할 수 없습니다. 잠시 후 다시 시도해 주세요."),
    POST_LOG_PHOTO_NOT_FOUND(HttpStatus.NOT_FOUND, "POST_LOG-E010", "사진을 찾을 수 없습니다."),
    INVALID_PHOTO_CURSOR(HttpStatus.BAD_REQUEST, "POST_LOG-E011", "사진 목록 커서가 올바르지 않습니다."),
    TICKET_ALREADY_CREATED(HttpStatus.CONFLICT, "POST_LOG-E012", "해당 만남에 이미 티켓을 생성했습니다."),
}
