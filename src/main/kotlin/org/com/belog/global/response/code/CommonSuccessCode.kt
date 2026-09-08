package org.com.belog.global.response.code

import org.springframework.http.HttpStatus

enum class CommonSuccessCode(
    override val status: HttpStatus,
    override val code: String,
    override val message: String,
) : SuccessCode {
    OK(HttpStatus.OK, "CMN-S001", "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "CMN-S002", "리소스가 생성되었습니다."),
}
