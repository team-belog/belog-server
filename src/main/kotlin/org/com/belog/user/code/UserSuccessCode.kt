package org.com.belog.user.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class UserSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    PROFILE_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "USER-S001", "프로필 이미지 업로드 URL이 발급되었습니다."),
}
