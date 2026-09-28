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
}
