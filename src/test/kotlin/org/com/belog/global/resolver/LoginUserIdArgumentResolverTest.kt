package org.com.belog.global.resolver

import org.com.belog.global.annotation.LoginUserId
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.code.CommonErrorCode
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.ServletWebRequest
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LoginUserIdArgumentResolverTest {
    private val resolver = LoginUserIdArgumentResolver()

    @Test
    fun `LoginUserId가 선언된 Long 파라미터를 지원한다`() {
        assertTrue(resolver.supportsParameter(methodParameter("loginUserId")))
    }

    @Test
    fun `LoginUserId가 없는 파라미터는 지원하지 않는다`() {
        assertFalse(resolver.supportsParameter(methodParameter("userId")))
    }

    @Test
    fun `LoginUserId가 선언되어도 Long 타입이 아니면 지원하지 않는다`() {
        assertFalse(resolver.supportsParameter(methodParameter("loginUserName")))
    }

    @Test
    fun `인증된 사용자의 식별자를 반환한다`() {
        val request = MockHttpServletRequest().apply { userPrincipal = { "15" } }

        val userId =
            resolver.resolveArgument(
                methodParameter("loginUserId"),
                null,
                ServletWebRequest(request),
                null,
            )

        assertEquals(15L, userId)
    }

    @Test
    fun `인증 정보가 없으면 인증 필요 예외가 발생한다`() {
        val exception = resolveFailure(null)

        assertEquals(CommonErrorCode.AUTHENTICATION_REQUIRED, exception.errorCode)
    }

    @Test
    fun `사용자 식별자가 숫자가 아니면 인증 필요 예외가 발생한다`() {
        val exception = resolveFailure("invalid")

        assertEquals(CommonErrorCode.AUTHENTICATION_REQUIRED, exception.errorCode)
    }

    @Test
    fun `사용자 식별자가 양수가 아니면 인증 필요 예외가 발생한다`() {
        val exception = resolveFailure("0")

        assertEquals(CommonErrorCode.AUTHENTICATION_REQUIRED, exception.errorCode)
    }

    private fun resolveFailure(principalName: String?): BusinessException {
        val request =
            MockHttpServletRequest().apply {
                if (principalName != null) {
                    userPrincipal = { principalName }
                }
            }

        return assertFailsWith {
            resolver.resolveArgument(
                methodParameter("loginUserId"),
                null,
                ServletWebRequest(request),
                null,
            )
        }
    }

    private fun methodParameter(methodName: String): MethodParameter {
        val method = TestController::class.java.declaredMethods.single { method -> method.name == methodName }
        return MethodParameter(method, 0)
    }

    @Suppress("UNUSED_PARAMETER")
    private class TestController {
        fun loginUserId(
            @LoginUserId userId: Long,
        ) = Unit

        fun userId(userId: Long) = Unit

        fun loginUserName(
            @LoginUserId userName: String,
        ) = Unit
    }
}
