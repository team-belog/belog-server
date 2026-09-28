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
}
