package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.com.belog.user.service.ProfileImageService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupMembershipServiceTest {
    @Autowired
    private lateinit var groupMembershipService: GroupMembershipService

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var profileImageService: ProfileImageService

    @AfterEach
    fun cleanUp() {
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `초대 코드로 그룹에 참여하면 MEMBER 역할의 멤버가 생성된다`() {
        val group = groupRepository.saveAndFlush(createGroup())
        val user = saveCompletedUser("joiner", "참여자")

        val result = groupMembershipService.joinGroup(requireNotNull(user.id), "AB12CD")

        val savedMember = groupMemberRepository.findAll().single()
        assertEquals(group.id, result.groupId)
        assertEquals(group.name, result.name)
        assertEquals(1, result.currentMemberCount)
        assertEquals(GroupRole.MEMBER, savedMember.role)
        assertEquals(group.id, savedMember.group.id)
        assertEquals(user.id, savedMember.user.id)
    }

    @Test
    fun `형식이 올바르지 않은 초대 코드는 거절한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(1L, "invalid")
            }

        assertEquals(GroupErrorCode.INVALID_INVITE_CODE, exception.errorCode)
    }

    @Test
    fun `초대 코드에 해당하는 그룹이 없으면 참여할 수 없다`() {
        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(1L, "AB12CD")
            }

        assertEquals(GroupErrorCode.GROUP_NOT_FOUND_BY_INVITE_CODE, exception.errorCode)
    }

    @Test
    fun `존재하지 않는 사용자는 그룹에 참여할 수 없다`() {
        groupRepository.saveAndFlush(createGroup())

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(999L, "AB12CD")
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자는 그룹에 참여할 수 없다`() {
        groupRepository.saveAndFlush(createGroup())
        val user = saveUser("joiner")

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(requireNotNull(user.id), "AB12CD")
            }

        assertEquals(GroupErrorCode.ONBOARDING_REQUIRED, exception.errorCode)
        assertEquals(0L, groupMemberRepository.count())
    }

    @Test
    fun `이미 참여한 사용자는 같은 그룹에 다시 참여할 수 없다`() {
        val group = groupRepository.saveAndFlush(createGroup())
        val user = saveCompletedUser("joiner", "참여자")
        groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(requireNotNull(user.id), "AB12CD")
            }

        assertEquals(GroupErrorCode.ALREADY_GROUP_MEMBER, exception.errorCode)
        assertEquals(1L, groupMemberRepository.count())
    }

    @Test
    fun `최대 인원에 도달한 그룹에는 참여할 수 없다`() {
        val group = groupRepository.saveAndFlush(createGroup())
        repeat(Group.MAX_MEMBER_COUNT) { index ->
            val member = saveCompletedUser("member-$index", "멤버$index")
            groupMemberRepository.save(GroupMember.createMember(group, member))
        }
        groupMemberRepository.flush()
        val joiningUser = saveCompletedUser("joiner", "참여자")

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.joinGroup(requireNotNull(joiningUser.id), "AB12CD")
            }

        assertEquals(GroupErrorCode.GROUP_MEMBER_LIMIT_EXCEEDED, exception.errorCode)
        assertEquals(Group.MAX_MEMBER_COUNT.toLong(), groupMemberRepository.count())
    }

    @Test
    fun `그룹 멤버를 OWNER 우선으로 조회하고 프로필 이미지 URL을 반환한다`() {
        val group = groupRepository.saveAndFlush(createGroup())
        val owner = saveCompletedUser("owner", "방장")
        val member =
            saveCompletedUser(
                providerUserId = "member",
                nickname = "멤버",
                hasUploadedProfileImage = false,
                socialProfileImageUrl = "https://example.com/social-profile",
            )
        val savedOwner = groupMemberRepository.save(GroupMember.createOwner(group, owner))
        val savedMember = groupMemberRepository.save(GroupMember.createMember(group, member))
        groupMemberRepository.flush()
        val ownerId = requireNotNull(owner.id)
        `when`(profileImageService.generateReadUrl(ownerId, owner.profileImageObjectKey))
            .thenReturn("https://example.com/s3-profile")
        `when`(profileImageService.generateReadUrl(requireNotNull(member.id), null)).thenReturn(null)

        val results = groupMembershipService.getGroupMembers(requireNotNull(group.id), ownerId)

        assertEquals(2, results.size)
        assertEquals(requireNotNull(savedOwner.id), results[0].groupMemberId)
        assertEquals("방장", results[0].nickname)
        assertEquals("https://example.com/s3-profile", results[0].profileImageUrl)
        assertEquals(GroupRole.OWNER, results[0].role)
        assertEquals(requireNotNull(savedMember.id), results[1].groupMemberId)
        assertEquals("멤버", results[1].nickname)
        assertEquals("https://example.com/social-profile", results[1].profileImageUrl)
        assertEquals(GroupRole.MEMBER, results[1].role)
        verify(profileImageService).generateReadUrl(ownerId, owner.profileImageObjectKey)
        verify(profileImageService).generateReadUrl(requireNotNull(member.id), null)
    }

    @Test
    fun `존재하지 않는 그룹의 멤버는 조회할 수 없다`() {
        val user = saveCompletedUser("requester", "요청자")

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.getGroupMembers(999L, requireNotNull(user.id))
            }

        assertEquals(GroupErrorCode.GROUP_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `그룹에 참여하지 않은 사용자는 멤버를 조회할 수 없다`() {
        val group = groupRepository.saveAndFlush(createGroup())
        val owner = saveCompletedUser("owner", "방장")
        val outsider = saveCompletedUser("outsider", "외부인")
        groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, owner))

        val exception =
            assertFailsWith<BusinessException> {
                groupMembershipService.getGroupMembers(requireNotNull(group.id), requireNotNull(outsider.id))
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
    }

    private fun createGroup(): Group =
        Group.create(
            name = "주말 러닝 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
        hasUploadedProfileImage: Boolean = true,
        socialProfileImageUrl: String? = null,
    ): User {
        val user = saveUser(providerUserId, socialProfileImageUrl)
        val userId = requireNotNull(user.id)
        user.completeOnboarding(
            profileImageObjectKey =
                if (hasUploadedProfileImage) {
                    ProfileImageObjectKey.create(userId, "users/$userId/profile/image.webp")
                } else {
                    null
                },
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

    private fun saveUser(
        providerUserId: String,
        socialProfileImageUrl: String? = null,
    ): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
                socialProfileImageUrl = socialProfileImageUrl,
            ),
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
