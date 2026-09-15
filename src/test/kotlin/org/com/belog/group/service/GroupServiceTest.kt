package org.com.belog.group.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.infrastructure.RandomInviteCodeGenerator
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupServiceTest {
    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @MockitoSpyBean
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var inviteCodeGenerator: RandomInviteCodeGenerator

    @AfterEach
    fun cleanUp() {
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `그룹을 생성하면 생성자가 최초 멤버이자 OWNER로 등록된다`() {
        val creator = saveCompletedUser("google-subject", "빌로그")
        `when`(inviteCodeGenerator.generate()).thenReturn(InviteCode.create("AB12CD"))

        val result =
            groupService.createGroup(
                creatorId = requireNotNull(creator.id),
                name = "  주말 러닝 모임  ",
                coverImageObjectKey = null,
            )

        val member = groupMemberRepository.findAll().single()
        assertEquals("주말 러닝 모임", result.name)
        assertNull(result.coverImageObjectKey)
        assertEquals(1, result.currentMemberCount)
        assertEquals("AB12CD", result.inviteCode)
        assertEquals("https://belog.example/invitations/AB12CD", result.inviteLink)
        assertEquals(result.groupId, member.group.id)
        assertEquals(creator.id, member.user.id)
        assertEquals(GroupRole.OWNER, member.role)
    }

    @Test
    fun `OWNER 멤버 저장에 실패하면 앞서 저장한 그룹도 롤백한다`() {
        val creator = saveCompletedUser("google-subject", "빌로그")
        `when`(inviteCodeGenerator.generate()).thenReturn(InviteCode.create("AB12CD"))
        doThrow(IllegalStateException("멤버 저장 실패"))
            .`when`(groupMemberRepository)
            .saveAndFlush(any())

        assertFailsWith<IllegalStateException> {
            groupService.createGroup(
                creatorId = requireNotNull(creator.id),
                name = "주말 러닝 모임",
                coverImageObjectKey = null,
            )
        }

        assertEquals(0L, groupRepository.count())
        assertEquals(0L, groupMemberRepository.count())
    }

    @Test
    fun `존재하지 않는 사용자는 그룹을 생성할 수 없다`() {
        `when`(inviteCodeGenerator.generate()).thenReturn(InviteCode.create("AB12CD"))

        val exception =
            assertFailsWith<BusinessException> {
                groupService.createGroup(
                    creatorId = 999L,
                    name = "주말 러닝 모임",
                    coverImageObjectKey = null,
                )
            }

        assertEquals(UserErrorCode.USER_NOT_FOUND, exception.errorCode)
        assertEquals(0L, groupRepository.count())
        assertEquals(0L, groupMemberRepository.count())
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자는 그룹을 생성할 수 없다`() {
        val creator = saveUser("google-subject")
        `when`(inviteCodeGenerator.generate()).thenReturn(InviteCode.create("AB12CD"))

        val exception =
            assertFailsWith<BusinessException> {
                groupService.createGroup(
                    creatorId = requireNotNull(creator.id),
                    name = "주말 러닝 모임",
                    coverImageObjectKey = null,
                )
            }

        assertEquals(GroupErrorCode.ONBOARDING_REQUIRED, exception.errorCode)
        assertEquals(0L, groupRepository.count())
        assertEquals(0L, groupMemberRepository.count())
    }

    @Test
    fun `초대 코드가 충돌하면 새로운 트랜잭션에서 다른 코드로 그룹 생성을 재시도한다`() {
        val creator = saveCompletedUser("google-subject", "빌로그")
        groupRepository.saveAndFlush(createGroup("AB12CD"))
        `when`(inviteCodeGenerator.generate())
            .thenReturn(InviteCode.create("AB12CD"), InviteCode.create("EF34GH"))

        val result =
            groupService.createGroup(
                creatorId = requireNotNull(creator.id),
                name = "주말 러닝 모임",
                coverImageObjectKey = null,
            )

        assertEquals("EF34GH", result.inviteCode)
        assertEquals(2L, groupRepository.count())
        assertEquals(1L, groupMemberRepository.count())
        verify(inviteCodeGenerator, times(2)).generate()
    }

    @Test
    fun `초대 코드 충돌 재시도 횟수를 소진하면 그룹 생성에 실패한다`() {
        val creator = saveCompletedUser("google-subject", "빌로그")
        groupRepository.saveAndFlush(createGroup("AB12CD"))
        `when`(inviteCodeGenerator.generate()).thenReturn(InviteCode.create("AB12CD"))

        val exception =
            assertFailsWith<BusinessException> {
                groupService.createGroup(
                    creatorId = requireNotNull(creator.id),
                    name = "주말 러닝 모임",
                    coverImageObjectKey = null,
                )
            }

        assertEquals(GroupErrorCode.INVITE_CODE_ISSUANCE_FAILED, exception.errorCode)
        assertEquals(1L, groupRepository.count())
        assertEquals(0L, groupMemberRepository.count())
        verify(inviteCodeGenerator, times(GroupService.MAX_INVITE_CODE_ATTEMPTS)).generate()
    }

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "기존 그룹",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
    ): User {
        val user = saveUser(providerUserId)
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

    private fun saveUser(providerUserId: String): User =
        userRepository.saveAndFlush(
            User.createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ),
        )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
