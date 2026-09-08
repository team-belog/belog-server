package org.com.belog.global.response

import io.swagger.v3.oas.annotations.media.Schema
import org.com.belog.global.response.code.ErrorCode
import org.com.belog.global.response.code.SuccessCode

@Schema(description = "API 공통 응답")
data class CommonResponse<T>(
    @field:Schema(description = "응답 코드", example = "CMN-S001")
    val code: String,
    @field:Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
    val message: String,
    @field:Schema(description = "응답 데이터")
    val data: T?,
) {
    companion object {
        fun <T> success(successCode: SuccessCode, data: T): CommonResponse<T> =
            CommonResponse(successCode.code, successCode.message, data)

        fun success(successCode: SuccessCode): CommonResponse<Nothing> =
            CommonResponse(successCode.code, successCode.message, null)

        fun <T> error(errorCode: ErrorCode, data: T): CommonResponse<T> =
            CommonResponse(errorCode.code, errorCode.message, data)

        fun error(errorCode: ErrorCode): CommonResponse<Nothing> =
            CommonResponse(errorCode.code, errorCode.message, null)
    }
}
