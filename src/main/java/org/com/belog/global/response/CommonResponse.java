package org.com.belog.global.response;

import org.com.belog.global.response.code.ErrorCode;
import org.com.belog.global.response.code.SuccessCode;

public record CommonResponse<T>(
        String code,
        String message,
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
