package org.com.belog.group.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class GroupCoverImageObjectKeyTest {
    @Test
    fun `현재 사용자의 그룹 커버 이미지 object key를 생성한다`() {
        val objectKey =
            GroupCoverImageObjectKey.create(
                userId = 15L,
                value = "group-covers/15/image.webp",
            )

        assertEquals("group-covers/15/image.webp", objectKey.value)
    }

    @Test
    fun `다른 사용자의 그룹 커버 이미지 경로는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            GroupCoverImageObjectKey.create(
                userId = 15L,
                value = "group-covers/16/image.webp",
            )
        }
    }

    @Test
    fun `상위 경로 이동이 포함된 object key는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            GroupCoverImageObjectKey.create(
                userId = 15L,
                value = "group-covers/15/../image.webp",
            )
        }
    }
}
