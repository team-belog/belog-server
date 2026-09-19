package org.com.belog.group.repository

import jakarta.persistence.EntityManager
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.domain.InviteCode
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.hibernate.Hibernate
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class GroupMemberRepositoryTest {
    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var entityManager: EntityManager

    @Test
    fun `그룹 ID와 사용자 ID로 그룹 멤버를 조회한다`() {
        val owner = saveCompletedUser("owner-subject", "방장")
        val otherUser = saveCompletedUser("other-subject", "다른사용자")
        val group = groupRepository.save(createGroup("AB12CD"))
        groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, owner))

        val foundMember =
            groupMemberRepository.findByGroupIdAndUserId(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(owner.id),
            )
        val missingMember =
            groupMemberRepository.findByGroupIdAndUserId(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(otherUser.id),
            )

        assertNotNull(foundMember)
        assertEquals(GroupRole.OWNER, foundMember.role)
        assertNull(missingMember)
    }

    @Test
    fun `그룹 멤버를 OWNER 우선 가입 순으로 사용자와 함께 조회한다`() {
        val firstMemberUser = saveCompletedUser("first-member-subject", "첫멤버")
        val ownerUser = saveCompletedUser("owner-subject", "방장")
        val secondMemberUser = saveCompletedUser("second-member-subject", "둘째멤버")
        val otherGroupOwner = saveCompletedUser("other-owner-subject", "다른방장")
        val group = groupRepository.save(createGroup("AB12CD"))
        val otherGroup = groupRepository.save(createGroup("EF34GH"))
        val firstMember = groupMemberRepository.save(GroupMember.createMember(group, firstMemberUser))
        val owner = groupMemberRepository.save(GroupMember.createOwner(group, ownerUser))
        val secondMember = groupMemberRepository.save(GroupMember.createMember(group, secondMemberUser))
        groupMemberRepository.save(GroupMember.createOwner(otherGroup, otherGroupOwner))
        groupMemberRepository.flush()
        entityManager.clear()

        val members = groupMemberRepository.findAllWithUserByGroupId(requireNotNull(group.id))

        assertEquals(
            listOf(requireNotNull(owner.id), requireNotNull(firstMember.id), requireNotNull(secondMember.id)),
            members.map { member -> member.id },
        )
        assertEquals(listOf("방장", "첫멤버", "둘째멤버"), members.map { member -> member.user.nickname })
        assertTrue(members.all { member -> Hibernate.isInitialized(member.user) })
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
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = nickname,
            bankAccount =
                BankAccount.create(
                    bank = Bank.KB_KOOKMIN,
                    accountNumber = "123456789012",
                    accountHolderName = "홍길동",
                ),
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }
}
