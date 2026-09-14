package org.com.belog.user.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ProfileImageObjectKeyTest {
    @Test
    fun `현재 사용자의 프로필 이미지 경로를 생성한다`() {
        val objectKey = ProfileImageObjectKey.create(1L, "users/1/profile/image.webp")

        assertEquals("users/1/profile/image.webp", objectKey.value)
    }

    @Test
    fun `다른 사용자의 프로필 이미지 경로는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ProfileImageObjectKey.create(1L, "users/2/profile/image.webp")
        }
    }

    @Test
    fun `프로필 이미지 파일 경로가 비어 있으면 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ProfileImageObjectKey.create(1L, "users/1/profile/")
        }
    }

    @Test
    fun `상위 경로 이동이 포함된 object key는 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            ProfileImageObjectKey.create(1L, "users/1/profile/../image.webp")
        }
    }
}
