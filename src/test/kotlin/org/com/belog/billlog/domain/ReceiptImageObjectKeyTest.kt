package org.com.belog.billlog.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReceiptImageObjectKeyTest {
    @Test
    fun `발급 형식과 같은 영수증 이미지 경로를 생성한다`() {
        val objectKey = ReceiptImageObjectKey.create(7L, 15L, "$PREFIX$IMAGE_ID.jpg")

        assertEquals("$PREFIX$IMAGE_ID.jpg", objectKey.value)
    }

    @Test
    fun `확장자 대소문자가 발급 형식과 다르면 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ReceiptImageObjectKey.create(7L, 15L, "$PREFIX$IMAGE_ID.JPG")
        }
    }

    @Test
    fun `파일 식별자 대소문자가 발급 형식과 다르면 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ReceiptImageObjectKey.create(7L, 15L, "$PREFIX${IMAGE_ID.uppercase()}.jpg")
        }
    }

    companion object {
        private const val PREFIX = "bill-log/receipts/7/15/"
        private const val IMAGE_ID = "550e8400-e29b-41d4-a716-446655440000"
    }
}
