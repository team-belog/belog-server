package org.com.belog.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.com.belog.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.Test;

class BusinessExceptionTest {

    @Test
    void errorCodeCreatesBusinessException() {
        BusinessException exception = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    void causeCreatesBusinessExceptionWithoutLosingOriginalException() {
        IllegalStateException cause = new IllegalStateException("original cause");

        BusinessException exception = new BusinessException(
                CommonErrorCode.INTERNAL_SERVER_ERROR,
                cause
        );

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR);
        assertThat(exception.getMessage()).isEqualTo("서버 내부 오류가 발생했습니다.");
        assertThat(exception.getCause()).isSameAs(cause);
    }
}
