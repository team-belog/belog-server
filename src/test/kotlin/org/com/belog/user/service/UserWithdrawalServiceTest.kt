package org.com.belog.user.service

import org.com.belog.auth.repository.RefreshTokenRepository
import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.Bill
import org.com.belog.billlog.domain.BillShare
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.BillShareRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.GroupRole
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.MeetingServiceTest
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(MeetingServiceTest.FixedClockConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class UserWithdrawalServiceTest {
    @Autowired
    private lateinit var userWithdrawalService: UserWithdrawalService

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var billShareRepository: BillShareRepository

    @Autowired
    private lateinit var settlementRequestRepository: SettlementRequestRepository

    @Autowired
    private lateinit var refreshTokenRepository: RefreshTokenRepository

    @AfterEach
    fun cleanUp() {
        settlementRequestRepository.deleteAllInBatch()
        billShareRepository.deleteAllInBatch()
        billRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        refreshTokenRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `방장이 아닌 일반 멤버가 탈퇴하면 계정과 그룹 멤버십만 소프트 삭제된다`() {
        val group = groupRepository.saveAndFlush(createGroup("AB12CD"))
        val owner = saveOwner(group, "owner", "방장")
        val member = saveGroupMember(group, "member", "멤버")

        userWithdrawalService.withdraw(requireNotNull(member.user.id))

        val reloadedUser = userRepository.findById(requireNotNull(member.user.id)).orElseThrow()
        val reloadedMember = groupMemberRepository.findById(requireNotNull(member.id)).orElseThrow()
        val reloadedOwner = groupMemberRepository.findById(requireNotNull(owner.id)).orElseThrow()
        val reloadedGroup = groupRepository.findById(requireNotNull(group.id)).orElseThrow()

        assertNotNull(reloadedUser.deletedAt)
        assertNotNull(reloadedMember.withdrawnAt)
        assertNull(reloadedOwner.withdrawnAt)
        assertEquals(GroupRole.OWNER, reloadedOwner.role)
        assertNull(reloadedGroup.deletedAt)
    }

    @Test
    fun `그룹 방장이 탈퇴하면 가장 먼저 참여한 멤버에게 위임되고 그룹은 유지된다`() {
        val group = groupRepository.saveAndFlush(createGroup("CD12EF"))
        val owner = saveOwner(group, "owner2", "방장")
        val successor = saveGroupMember(group, "successor", "후계자")

        userWithdrawalService.withdraw(requireNotNull(owner.user.id))

        val reloadedGroup = groupRepository.findById(requireNotNull(group.id)).orElseThrow()
        val reloadedOwner = groupMemberRepository.findById(requireNotNull(owner.id)).orElseThrow()
        val reloadedSuccessor = groupMemberRepository.findById(requireNotNull(successor.id)).orElseThrow()

        assertNull(reloadedGroup.deletedAt)
        assertEquals(GroupRole.MEMBER, reloadedOwner.role)
        assertEquals(GroupRole.OWNER, reloadedSuccessor.role)
        assertNotNull(reloadedOwner.withdrawnAt)
    }

    @Test
    fun `그룹의 마지막 활성 멤버가 탈퇴하면 그룹이 삭제된다`() {
        val group = groupRepository.saveAndFlush(createGroup("EF12GH"))
        val owner = saveOwner(group, "owner3", "방장")

        userWithdrawalService.withdraw(requireNotNull(owner.user.id))

        val reloadedGroup = groupRepository.findById(requireNotNull(group.id)).orElseThrow()
        assertNotNull(reloadedGroup.deletedAt)
    }

    @Test
    fun `여정 방장이 탈퇴하면 참여자에게 위임되고 최초 생성자 정보는 유지된다`() {
        val group = groupRepository.saveAndFlush(createGroup("GH12IJ"))
        val owner = saveOwner(group, "owner4", "방장")
        val participant = saveGroupMember(group, "participant", "참여자")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = owner,
                    name = "광주 여행",
                    location = null,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        meetingParticipantRepository.saveAllAndFlush(
            listOf(MeetingParticipant.create(meeting, owner), MeetingParticipant.create(meeting, participant)),
        )

        userWithdrawalService.withdraw(requireNotNull(owner.user.id))

        val reloadedMeeting = meetingRepository.findById(requireNotNull(meeting.id)).orElseThrow()
        assertEquals(participant.id, reloadedMeeting.owner.id)
        assertEquals(owner.id, reloadedMeeting.createdBy.id)
    }

    @Test
    fun `보내야 할 정산이 남아있으면 탈퇴가 거부되고 아무 것도 변경되지 않는다`() {
        val context = saveBillContext()
        // sender owes the payer -> sender withdrawal must be blocked
        settlementRequestRepository.saveAndFlush(SettlementRequest.create(context.share))

        val exception =
            assertFailsWith<BusinessException> {
                userWithdrawalService.withdraw(requireNotNull(context.senderMember.user.id))
            }

        assertEquals(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS, exception.errorCode)
        assertUnchanged(context)
    }

    @Test
    fun `받아야 할 정산이 남아있으면 탈퇴가 거부되고 아무 것도 변경되지 않는다`() {
        val context = saveBillContext()
        settlementRequestRepository.saveAndFlush(SettlementRequest.create(context.share))

        // payer is the receiver of the pending settlement -> payer withdrawal must be blocked too
        val exception =
            assertFailsWith<BusinessException> {
                userWithdrawalService.withdraw(requireNotNull(context.payerMember.user.id))
            }

        assertEquals(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS, exception.errorCode)
        assertUnchanged(context)
    }

    private fun assertUnchanged(context: BillContext) {
        val payer = userRepository.findById(requireNotNull(context.payerMember.user.id)).orElseThrow()
        val sender = userRepository.findById(requireNotNull(context.senderMember.user.id)).orElseThrow()
        val payerMember = groupMemberRepository.findById(requireNotNull(context.payerMember.id)).orElseThrow()
        val senderMember = groupMemberRepository.findById(requireNotNull(context.senderMember.id)).orElseThrow()
        val group = groupRepository.findById(requireNotNull(context.groupId)).orElseThrow()

        assertTrue(payer.isActive)
        assertTrue(sender.isActive)
        assertNull(payerMember.withdrawnAt)
        assertNull(senderMember.withdrawnAt)
        assertNull(group.deletedAt)
    }

    private fun saveBillContext(): BillContext {
        val group = groupRepository.saveAndFlush(createGroup("IJ12KL"))
        val payerMember = saveOwner(group, "payer", "결제자")
        val senderMember = saveGroupMember(group, "sender", "정산자")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = payerMember,
                    name = "저녁 모임",
                    location = null,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        val payerParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, payerMember))
        val senderParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, senderMember))
        val bill =
            billRepository.saveAndFlush(
                Bill.create(
                    meeting = meeting,
                    creator = payerMember,
                    payer = payerParticipant,
                    title = "저녁 식사",
                    totalAmount = 10_000L,
                    splitType = BillSplitType.EQUAL_SPLIT,
                ),
            )
        val share =
            billShareRepository.saveAndFlush(
                BillShare.create(bill = bill, participant = senderParticipant, amount = 10_000L, allocationOrder = 0),
            )

        return BillContext(
            groupId = requireNotNull(group.id),
            payerMember = payerMember,
            senderMember = senderMember,
            share = share,
        )
    }

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun saveOwner(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember = groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, saveCompletedUser(providerUserId, nickname)))

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, saveCompletedUser(providerUserId, nickname)))

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
            name = "홍길동",
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

    private data class BillContext(
        val groupId: Long,
        val payerMember: GroupMember,
        val senderMember: GroupMember,
        val share: BillShare,
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
