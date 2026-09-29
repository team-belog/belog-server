package org.com.belog.postlog.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class PostLogSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    PHOTO_UPLOAD_URLS_ISSUED(HttpStatus.OK, "POST_LOG-S001", "사진 업로드 URL이 발급되었습니다."),
    PHOTOS_REGISTERED(HttpStatus.CREATED, "POST_LOG-S002", "사진 메타데이터가 등록되었습니다."),
}
