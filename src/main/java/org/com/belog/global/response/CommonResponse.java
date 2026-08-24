package org.com.belog.global.response;

import io.swagger.v3.oas.annotations.media.Schema;
import org.com.belog.global.response.code.ErrorCode;
import org.com.belog.global.response.code.SuccessCode;

@Schema(description = "API 공통 응답")
public record CommonResponse<T>(
        @Schema(description = "응답 코드", example = "CMN-S001")
        String code,
        @Schema(description = "응답 메시지", example = "요청에 성공했습니다.")
        String message,
        @Schema(description = "응답 데이터")
        T data
) {

    public static <T> CommonResponse<T> success(SuccessCode successCode, T data) {
        return new CommonResponse<>(successCode.getCode(), successCode.getMessage(), data);
    }

    public static CommonResponse<Void> success(SuccessCode successCode) {
        return success(successCode, null);
    }

    public static <T> CommonResponse<T> error(ErrorCode errorCode, T data) {
        return new CommonResponse<>(errorCode.getCode(), errorCode.getMessage(), data);
    }

    public static CommonResponse<Void> error(ErrorCode errorCode) {
        return error(errorCode, null);
    }
}
