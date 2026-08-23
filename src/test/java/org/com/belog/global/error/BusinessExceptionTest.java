package org.com.belog.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.com.belog.global.response.code.CommonErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {

    @Test
    @DisplayName("에러 코드로 비즈니스 예외를 생성한다")
    void errorCodeCreatesBusinessException() {
        BusinessException exception = new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND);

        assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
        assertThat(exception.getMessage()).isEqualTo("요청한 리소스를 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("원인 예외를 유지하면서 비즈니스 예외를 생성한다")
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
