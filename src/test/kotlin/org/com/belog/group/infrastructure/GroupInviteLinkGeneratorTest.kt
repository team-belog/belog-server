package org.com.belog.group.infrastructure

import org.com.belog.group.config.GroupInviteProperties
import org.com.belog.group.domain.InviteCode
import org.junit.jupiter.api.Test
import java.net.URI
import kotlin.test.assertEquals

class GroupInviteLinkGeneratorTest {
    @Test
    fun `초대 링크 기본 URL 경로 뒤에 초대 코드를 추가한다`() {
        val generator =
            GroupInviteLinkGenerator(
                GroupInviteProperties(URI.create("https://belog.example/invitations")),
            )

        val inviteLink = generator.generate(InviteCode.create("AB12CD"))

        assertEquals("https://belog.example/invitations/AB12CD", inviteLink)
    }

    @Test
    fun `기본 URL 끝에 슬래시가 있어도 중복 슬래시 없이 링크를 생성한다`() {
        val generator =
            GroupInviteLinkGenerator(
                GroupInviteProperties(URI.create("https://belog.example/invitations/")),
            )

        val inviteLink = generator.generate(InviteCode.create("AB12CD"))

        assertEquals("https://belog.example/invitations/AB12CD", inviteLink)
    }
}
