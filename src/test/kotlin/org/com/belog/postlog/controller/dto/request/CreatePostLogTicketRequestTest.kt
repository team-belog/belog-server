package org.com.belog.postlog.controller.dto.request

import jakarta.validation.Validation
import org.com.belog.postlog.domain.PostLog
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CreatePostLogTicketRequestTest {
    private val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `앞뒤 공백을 제외한 추억 문구가 최대 길이면 검증에 성공한다`() {
        val request = CreatePostLogTicketRequest(memory = " ${"가".repeat(PostLog.MEMORY_MAX_LENGTH)} ")

        assertTrue(validator.validate(request).none { violation -> violation.propertyPath.toString() == "memory" })
    }

    @Test
    fun `앞뒤 공백을 제외한 추억 문구가 최대 길이를 초과하면 검증에 실패한다`() {
        val request = CreatePostLogTicketRequest(memory = " ${"가".repeat(PostLog.MEMORY_MAX_LENGTH + 1)} ")

        assertFalse(validator.validate(request).none { violation -> violation.propertyPath.toString() == "memory" })
    }

    @Test
    fun `추억 문구가 공백뿐이면 검증에 실패한다`() {
        val request = CreatePostLogTicketRequest(memory = "   ")

        assertFalse(validator.validate(request).none { violation -> violation.propertyPath.toString() == "memory" })
    }
}
