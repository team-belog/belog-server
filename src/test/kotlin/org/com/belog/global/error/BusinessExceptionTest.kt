package org.com.belog.global.error

import org.assertj.core.api.Assertions.assertThat
import org.com.belog.global.response.code.CommonErrorCode
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("BusinessException 테스트")
class BusinessExceptionTest {
    @Test
    @DisplayName("에러 코드로 비즈니스 예외를 생성한다")
    fun errorCodeCreatesBusinessException() {
        val exception = BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND)

        assertThat(exception.errorCode).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND)
        assertThat(exception.message).isEqualTo("요청한 리소스를 찾을 수 없습니다.")
    }

    @Test
    @DisplayName("원인 예외를 유지하면서 비즈니스 예외를 생성한다")
    fun causeCreatesBusinessExceptionWithoutLosingOriginalException() {
        val cause = IllegalStateException("original cause")
        val exception = BusinessException(CommonErrorCode.INTERNAL_SERVER_ERROR, cause)

        assertThat(exception.errorCode).isEqualTo(CommonErrorCode.INTERNAL_SERVER_ERROR)
        assertThat(exception.message).isEqualTo("서버 내부 오류가 발생했습니다.")
        assertThat(exception.cause).isSameAs(cause)
    }
}
