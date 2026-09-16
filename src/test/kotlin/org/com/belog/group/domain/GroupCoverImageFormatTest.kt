package org.com.belog.group.domain

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GroupCoverImageFormatTest {
    @Test
    fun `지원하는 Content-Type에 해당하는 이미지 형식을 반환한다`() {
        assertEquals(GroupCoverImageFormat.JPEG, GroupCoverImageFormat.fromContentType("image/jpeg"))
        assertEquals(GroupCoverImageFormat.PNG, GroupCoverImageFormat.fromContentType("image/png"))
        assertEquals(GroupCoverImageFormat.WEBP, GroupCoverImageFormat.fromContentType("image/webp"))
    }

    @Test
    fun `Content-Type의 앞뒤 공백과 대소문자를 정규화한다`() {
        assertEquals(GroupCoverImageFormat.WEBP, GroupCoverImageFormat.fromContentType(" IMAGE/WEBP "))
    }

    @Test
    fun `지원하지 않는 Content-Type이면 null을 반환한다`() {
        assertNull(GroupCoverImageFormat.fromContentType("image/gif"))
    }
}
