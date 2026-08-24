package org.com.belog.global.response.code;

import org.springframework.http.HttpStatus;

public interface ResponseCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
