package org.com.belog.global.response.code;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum CommonSuccessCode implements SuccessCode {

    OK(HttpStatus.OK, "CMN-S001", "요청이 성공했습니다."),
    CREATED(HttpStatus.CREATED, "CMN-S002", "리소스가 생성되었습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
