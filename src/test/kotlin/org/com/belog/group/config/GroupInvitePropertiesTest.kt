package org.com.belog.group.config

import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertFailsWith

class GroupInvitePropertiesTest {
    @Test
    fun `초대 링크 기본 URL은 절대 HTTP URL이어야 한다`() {
        assertFailsWith<IllegalArgumentException> {
            GroupInviteProperties(URI.create("/invitations"))
        }
        assertFailsWith<IllegalArgumentException> {
            GroupInviteProperties(URI.create("ftp://belog.example/invitations"))
        }
    }

    @Test
    fun `초대 링크 기본 URL에는 쿼리와 fragment를 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> {
            GroupInviteProperties(URI.create("https://belog.example/invitations?source=share"))
        }
        assertFailsWith<IllegalArgumentException> {
            GroupInviteProperties(URI.create("https://belog.example/invitations#invite"))
        }
    }
}
