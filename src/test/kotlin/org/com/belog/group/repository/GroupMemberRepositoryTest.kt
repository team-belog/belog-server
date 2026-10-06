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
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.util.ReflectionTestUtils
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
    fun `내 그룹을 고정 우선 최근 가입 순으로 조회하고 다른 사용자의 그룹은 제외한다`() {
        val user = saveCompletedUser("requester-subject", "요청자")
        val otherUser = saveCompletedUser("other-subject", "다른사용자")
        val unpinnedOlder = saveOwner(createGroup("AB12CD"), user)
        val pinnedOlder = saveOwner(createGroup("EF34GH"), user, pinned = true)
        saveOwner(createGroup("IJ56KL"), otherUser, pinned = true)
        val unpinnedNewer = saveOwner(createGroup("MN78OP"), user)
        val pinnedNewer = saveOwner(createGroup("QR90ST"), user, pinned = true)
        entityManager.clear()

        val memberships =
            groupMemberRepository.findMyGroupPage(
                userId = requireNotNull(user.id),
                cursorPinned = null,
                cursorId = null,
                pageable = PageRequest.of(0, 10),
            )

        assertEquals(
            listOf(pinnedNewer.id, pinnedOlder.id, unpinnedNewer.id, unpinnedOlder.id),
            memberships.map { membership -> membership.id },
        )
    }

    @Test
    fun `고정 그룹의 마지막 커서부터 일반 그룹 끝까지 중복과 누락 없이 조회한다`() {
        val user = saveCompletedUser("requester-subject", "요청자")
        val unpinnedOldest = saveOwner(createGroup("AB12CD"), user)
        val pinnedOlder = saveOwner(createGroup("EF34GH"), user, pinned = true)
        val unpinnedOlder = saveOwner(createGroup("IJ56KL"), user)
        val pinnedNewer = saveOwner(createGroup("MN78OP"), user, pinned = true)
        val unpinnedNewest = saveOwner(createGroup("QR90ST"), user)
        entityManager.clear()

        val firstPage = findMyGroupPage(user, cursor = null, size = 2)
        val secondPage = findMyGroupPage(user, cursor = firstPage.last(), size = 2)
        val thirdPage = findMyGroupPage(user, cursor = secondPage.last(), size = 2)

        assertEquals(listOf(pinnedNewer.id, pinnedOlder.id), firstPage.map { membership -> membership.id })
        assertEquals(listOf(unpinnedNewest.id, unpinnedOlder.id), secondPage.map { membership -> membership.id })
        assertEquals(listOf(unpinnedOldest.id), thirdPage.map { membership -> membership.id })
        assertEquals(
            listOf(pinnedNewer.id, pinnedOlder.id, unpinnedNewest.id, unpinnedOlder.id, unpinnedOldest.id),
            (firstPage + secondPage + thirdPage).map { membership -> membership.id },
        )
    }

    @Test
    fun `그룹 ID와 사용자 ID로 그룹 멤버를 조회한다`() {
        val owner = saveCompletedUser("owner-subject", "방장")
        val otherUser = saveCompletedUser("other-subject", "다른사용자")
        val group = groupRepository.save(createGroup("AB12CD"))
        groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, owner))

        val foundMember =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(owner.id),
            )
        val missingMember =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(otherUser.id),
            )

        assertNotNull(foundMember)
        assertEquals(GroupRole.OWNER, foundMember.role)
        assertNull(missingMember)
    }

    @Test
    fun `탈퇴한 그룹 멤버는 조회되지 않는다`() {
        val user = saveCompletedUser("withdrawn-subject", "탈퇴자")
        val group = groupRepository.save(createGroup("AB12CD"))
        val member = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
        member.withdraw(Instant.parse("2026-10-07T00:00:00Z"))
        groupMemberRepository.saveAndFlush(member)

        val foundMember =
            groupMemberRepository.findByGroupIdAndUserIdAndWithdrawnAtIsNull(
                groupId = requireNotNull(group.id),
                userId = requireNotNull(user.id),
            )

        assertNull(foundMember)
    }

    @Test
    fun `그룹 ID와 멤버 ID 목록에 모두 해당하는 그룹 멤버만 조회한다`() {
        val ownerUser = saveCompletedUser("owner-subject", "방장")
        val memberUser = saveCompletedUser("member-subject", "멤버")
        val otherGroupOwnerUser = saveCompletedUser("other-owner-subject", "다른방장")
        val group = groupRepository.save(createGroup("AB12CD"))
        val otherGroup = groupRepository.save(createGroup("EF34GH"))
        val owner = groupMemberRepository.save(GroupMember.createOwner(group, ownerUser))
        val member = groupMemberRepository.save(GroupMember.createMember(group, memberUser))
        val otherGroupOwner = groupMemberRepository.save(GroupMember.createOwner(otherGroup, otherGroupOwnerUser))
        groupMemberRepository.flush()

        val members =
            groupMemberRepository.findAllByGroupIdAndIdInAndWithdrawnAtIsNull(
                groupId = requireNotNull(group.id),
                memberIds =
                    listOf(
                        requireNotNull(member.id),
                        requireNotNull(otherGroupOwner.id),
                    ),
            )

        assertEquals(listOf(member.id), members.map { groupMember -> groupMember.id })
        assertTrue(members.none { groupMember -> groupMember.id == owner.id })
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

    @Test
    fun `이름 또는 닉네임에 검색어가 포함된 그룹 멤버를 조회한다`() {
        val nicknameMatchedUser = saveCompletedUser("nickname-matched-subject", "정원이", "김철수")
        val nameMatchedUser = saveCompletedUser("name-matched-subject", "다빈", "이정원")
        val unmatchedUser = saveCompletedUser("unmatched-subject", "성연", "박민수")
        val group = groupRepository.save(createGroup("AB12CD"))
        val nicknameMatchedMember = groupMemberRepository.save(GroupMember.createMember(group, nicknameMatchedUser))
        val nameMatchedMember = groupMemberRepository.save(GroupMember.createMember(group, nameMatchedUser))
        groupMemberRepository.save(GroupMember.createMember(group, unmatchedUser))
        groupMemberRepository.flush()

        val members =
            groupMemberRepository.searchAllWithUserByGroupId(
                groupId = requireNotNull(group.id),
                query = "정원",
            )

        assertEquals(
            listOf(requireNotNull(nicknameMatchedMember.id), requireNotNull(nameMatchedMember.id)),
            members.map { member -> member.id },
        )
    }

    @Test
    fun `영문 대소문자를 구분하지 않고 그룹 멤버를 검색한다`() {
        val user = saveCompletedUser("member-subject", "JuNe", "홍길동")
        val group = groupRepository.save(createGroup("AB12CD"))
        val member = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))

        val members =
            groupMemberRepository.searchAllWithUserByGroupId(
                groupId = requireNotNull(group.id),
                query = "jUnE",
            )

        assertEquals(listOf(requireNotNull(member.id)), members.map { groupMember -> groupMember.id })
    }

    @Test
    fun `퍼센트와 밑줄을 일반 문자로 검색한다`() {
        val percentUser = saveCompletedUser("percent-subject", "퍼센트%", "김철수")
        val underscoreUser = saveCompletedUser("underscore-subject", "밑줄_", "이정원")
        val ordinaryUser = saveCompletedUser("ordinary-subject", "일반", "박민수")
        val group = groupRepository.save(createGroup("AB12CD"))
        val percentMember = groupMemberRepository.save(GroupMember.createMember(group, percentUser))
        val underscoreMember = groupMemberRepository.save(GroupMember.createMember(group, underscoreUser))
        groupMemberRepository.save(GroupMember.createMember(group, ordinaryUser))
        groupMemberRepository.flush()

        val percentMatches =
            groupMemberRepository.searchAllWithUserByGroupId(
                groupId = requireNotNull(group.id),
                query = "%",
            )
        val underscoreMatches =
            groupMemberRepository.searchAllWithUserByGroupId(
                groupId = requireNotNull(group.id),
                query = "_",
            )

        assertEquals(listOf(requireNotNull(percentMember.id)), percentMatches.map { member -> member.id })
        assertEquals(listOf(requireNotNull(underscoreMember.id)), underscoreMatches.map { member -> member.id })
    }

    @Test
    fun `검색 결과에 다른 그룹의 멤버를 포함하지 않는다`() {
        val memberUser = saveCompletedUser("member-subject", "정원이", "김철수")
        val otherGroupMemberUser = saveCompletedUser("other-member-subject", "정원친구", "이정원")
        val group = groupRepository.save(createGroup("AB12CD"))
        val otherGroup = groupRepository.save(createGroup("EF34GH"))
        val member = groupMemberRepository.save(GroupMember.createMember(group, memberUser))
        groupMemberRepository.save(GroupMember.createMember(otherGroup, otherGroupMemberUser))
        groupMemberRepository.flush()

        val members =
            groupMemberRepository.searchAllWithUserByGroupId(
                groupId = requireNotNull(group.id),
                query = "정원",
            )

        assertEquals(listOf(requireNotNull(member.id)), members.map { groupMember -> groupMember.id })
    }

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 러닝 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun saveOwner(
        group: Group,
        user: User,
        pinned: Boolean = false,
    ): GroupMember {
        val savedGroup = groupRepository.save(group)
        val member = GroupMember.createOwner(savedGroup, user)
        ReflectionTestUtils.setField(member, "pinned", pinned)
        return groupMemberRepository.saveAndFlush(member)
    }

    private fun findMyGroupPage(
        user: User,
        cursor: GroupMember?,
        size: Int,
    ): List<GroupMember> =
        groupMemberRepository.findMyGroupPage(
            userId = requireNotNull(user.id),
            cursorPinned = cursor?.pinned,
            cursorId = cursor?.id,
            pageable = PageRequest.of(0, size),
        )

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
        name: String = "홍길동",
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
            name = name,
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
