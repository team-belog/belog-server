package org.com.belog.group.domain

import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GroupTest {
    @Test
    fun `그룹명 앞뒤 공백을 제거하고 커버 이미지가 없는 그룹을 생성한다`() {
        val group =
            Group.create(
                name = "  주말 러닝 모임  ",
                coverImageObjectKey = null,
                inviteCode = InviteCode.create("AB12CD"),
            )

        assertEquals("주말 러닝 모임", group.name)
        assertNull(group.coverImageObjectKey)
        assertEquals("AB12CD", group.inviteCode)
        assertEquals(15, Group.MAX_MEMBER_COUNT)
    }

    @Test
    fun `그룹명이 비어 있거나 20자를 초과하면 허용하지 않는다`() {
        assertFailsWith<IllegalArgumentException> { createGroup("   ") }
        assertFailsWith<IllegalArgumentException> { createGroup("가".repeat(21)) }
    }

    @Test
    fun `온보딩 완료 사용자를 그룹 방장으로 생성한다`() {
        val groupMember = GroupMember.createOwner(createGroup("러닝 모임"), completedUser())

        assertEquals(GroupRole.OWNER, groupMember.role)
    }

    @Test
    fun `온보딩 미완료 사용자는 그룹 방장이 될 수 없다`() {
        val user =
            User.createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            )

        assertFailsWith<IllegalArgumentException> {
            GroupMember.createOwner(createGroup("러닝 모임"), user)
        }
    }

    private fun createGroup(name: String): Group =
        Group.create(
            name = name,
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun completedUser(): User =
        User
            .createSocialUser(
                email = "user@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = "google-subject",
            ).apply {
                completeOnboarding(
                    profileImageObjectKey = ProfileImageObjectKey.create(1L, "users/1/profile/image.webp"),
                    nickname = "빌로그",
                    bankAccount =
                        BankAccount.create(
                            bank = Bank.KB_KOOKMIN,
                            accountNumber = "123456789012",
                            accountHolderName = "홍길동",
                        ),
                    completedAt = Instant.parse("2026-09-15T00:00:00Z"),
                )
            }
}
