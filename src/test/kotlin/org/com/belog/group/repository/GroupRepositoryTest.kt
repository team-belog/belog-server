package org.com.belog.group.repository

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.domain.InviteCode
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class GroupRepositoryTest {
    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `그룹 생성자를 OWNER 역할의 멤버로 저장한다`() {
        val user = saveCompletedUser("google-subject", "빌로그")
        val group = groupRepository.save(createGroup("AB12CD"))

        val member = groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, user))

        assertEquals(GroupRole.OWNER, member.role)
        assertEquals(1L, groupMemberRepository.countByGroupId(requireNotNull(group.id)))
        assertTrue(
            groupMemberRepository.existsByGroupIdAndUserId(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(user.id),
            ),
        )
        assertTrue(groupRepository.existsByInviteCode("AB12CD"))
    }

    @Test
    fun `동일한 초대 코드를 가진 그룹은 저장할 수 없다`() {
        groupRepository.saveAndFlush(createGroup("AB12CD"))

        assertFailsWith<DataIntegrityViolationException> {
            groupRepository.saveAndFlush(createGroup("AB12CD"))
        }
    }

    @Test
    fun `초대 코드로 그룹을 비관적 락 조회한다`() {
        val savedGroup = groupRepository.saveAndFlush(createGroup("AB12CD"))

        val foundGroup = groupRepository.findByInviteCodeForUpdate("AB12CD")

        assertEquals(savedGroup.id, foundGroup?.id)
        assertEquals("AB12CD", foundGroup?.inviteCode)
    }

    @Test
    fun `한 사용자는 같은 그룹에 중복으로 가입할 수 없다`() {
        val user = saveCompletedUser("google-subject", "빌로그")
        val group = groupRepository.save(createGroup("AB12CD"))
        groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, user))

        assertFailsWith<DataIntegrityViolationException> {
            groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
        }
    }

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 러닝 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
    ): User {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$providerUserId@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = providerUserId,
                ),
            )
        val userId = requireNotNull(user.id)
        user.completeOnboarding(
            profileImageObjectKey = ProfileImageObjectKey.create(userId, "users/$userId/profile/image.webp"),
            nickname = nickname,
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-15T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }
}
