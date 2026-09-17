package org.com.belog.group.code

import org.com.belog.global.response.code.SuccessCode
import org.springframework.http.HttpStatus

enum class GroupSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    GROUP_CREATED(HttpStatus.CREATED, "GROUP-S001", "그룹이 생성되었습니다."),
    COVER_IMAGE_UPLOAD_URL_ISSUED(HttpStatus.OK, "GROUP-S002", "그룹 커버 이미지 업로드 URL이 발급되었습니다."),
}
