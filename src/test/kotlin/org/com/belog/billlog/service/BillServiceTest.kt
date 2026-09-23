package org.com.belog.billlog.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillItemRepository
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.BillShareRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
@ActiveProfiles("test")
@Import(
    BillService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class BillServiceTest {
    @Autowired
    private lateinit var billService: BillService

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var billItemRepository: BillItemRepository

    @Autowired
    private lateinit var billShareRepository: BillShareRepository

    @Autowired
    private lateinit var settlementRequestRepository: SettlementRequestRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Test
    fun `그룹 멤버가 결제 내역을 등록하면 항목과 개인별 부담 금액 및 정산 요청이 저장된다`() {
        val context = saveMeetingContext()

        val result = billService.registerBill(createCommand(context))

        assertEquals(1L, billRepository.count())
        assertEquals(2L, billItemRepository.count())
        assertEquals(2L, billShareRepository.count())
        assertEquals(1L, settlementRequestRepository.count())

        val bill = billRepository.findAll().single()
        assertEquals(bill.id, result.billId)
        assertEquals("아랑이 카페", bill.title)
        assertEquals(11_000L, bill.totalAmount)
        assertEquals(BillSplitType.EQUAL_SPLIT, bill.splitType)
        assertEquals(context.creator.id, bill.createdBy.id)
        assertEquals(context.payerParticipant.id, bill.payer.id)

        val items = billItemRepository.findAll().sortedBy { item -> item.itemOrder }
        assertEquals(listOf("아메리카노", "프라푸치노"), items.map { item -> item.name })
        assertEquals(listOf(4_000L, 7_000L), items.map { item -> item.amount })
        assertEquals(listOf(0, 1), items.map { item -> item.itemOrder })

        val shares = billShareRepository.findAll().sortedBy { share -> share.allocationOrder }
        assertEquals(
            listOf(context.payerParticipant.id, context.memberParticipant.id),
            shares.map { share -> share.participant.id },
        )
        assertEquals(listOf(4_000L, 7_000L), shares.map { share -> share.amount })
        assertEquals(listOf(0, 1), shares.map { share -> share.allocationOrder })

        val settlementRequest = settlementRequestRepository.findAll().single()
        assertEquals(context.memberParticipant.id, settlementRequest.participant.id)
        assertEquals(7_000L, settlementRequest.amount)
        assertEquals(SettlementRequestStatus.PENDING, settlementRequest.status)
    }

    @Test
    fun `결제자의 부담 금액에는 정산 요청을 생성하지 않는다`() {
        val context = saveMeetingContext()
        val thirdMember = saveGroupMember(context.group, "third-subject", "세번째")
        val thirdParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(context.meeting, thirdMember))
        val command =
            createCommand(
                context = context,
                items = listOf(BillItemCommand("식사", 12_000L)),
                shares =
                    listOf(
                        BillShareCommand(requireNotNull(context.creator.id), 4_000L),
                        BillShareCommand(requireNotNull(context.member.id), 3_000L),
                        BillShareCommand(requireNotNull(thirdMember.id), 5_000L),
                    ),
                totalAmount = 12_000L,
            )

        billService.registerBill(command)

        val settlementRequests = settlementRequestRepository.findAll()
        assertEquals(2, settlementRequests.size)
        assertEquals(
            setOf(context.memberParticipant.id, thirdParticipant.id),
            settlementRequests.map { request -> request.participant.id }.toSet(),
        )
    }

    @Test
    fun `결제 항목 합계가 결제 총액과 다르면 등록할 수 없다`() {
        val context = saveMeetingContext()
        val command =
            createCommand(
                context = context,
                items = listOf(BillItemCommand("아메리카노", 10_000L)),
            )

        val exception = assertFailsWith<BusinessException> { billService.registerBill(command) }

        assertEquals(BillLogErrorCode.ITEM_TOTAL_MISMATCH, exception.errorCode)
        assertBillDataIsEmpty()
    }

    @Test
    fun `개인별 부담 금액 합계가 결제 총액과 다르면 등록할 수 없다`() {
        val context = saveMeetingContext()
        val command =
            createCommand(
                context = context,
                shares =
                    listOf(
                        BillShareCommand(requireNotNull(context.creator.id), 4_000L),
                        BillShareCommand(requireNotNull(context.member.id), 6_000L),
                    ),
            )

        val exception = assertFailsWith<BusinessException> { billService.registerBill(command) }

        assertEquals(BillLogErrorCode.SHARE_TOTAL_MISMATCH, exception.errorCode)
        assertBillDataIsEmpty()
    }

    @Test
    fun `해당 만남이 속한 그룹의 멤버가 아니면 결제 내역을 등록할 수 없다`() {
        val context = saveMeetingContext()
        val outsider = saveCompletedUser("outsider-subject", "외부인")
        val command = createCommand(context).copy(creatorUserId = requireNotNull(outsider.id))

        val exception = assertFailsWith<BusinessException> { billService.registerBill(command) }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        assertBillDataIsEmpty()
    }

    @Test
    fun `결제자가 해당 만남의 참여자가 아니면 결제 내역을 등록할 수 없다`() {
        val context = saveMeetingContext()
        val nonParticipant = saveGroupMember(context.group, "payer-subject", "비참여자")
        val command =
            createCommand(
                context = context,
                payerMemberId = requireNotNull(nonParticipant.id),
                shares = listOf(BillShareCommand(requireNotNull(context.member.id), 11_000L)),
            )

        val exception = assertFailsWith<BusinessException> { billService.registerBill(command) }

        assertEquals(BillLogErrorCode.PAYER_NOT_MEETING_PARTICIPANT, exception.errorCode)
        assertBillDataIsEmpty()
    }

    @Test
    fun `부담자가 해당 만남의 참여자가 아니면 결제 내역을 등록할 수 없다`() {
        val context = saveMeetingContext()
        val nonParticipant = saveGroupMember(context.group, "share-subject", "비참여자")
        val command =
            createCommand(
                context = context,
                shares = listOf(BillShareCommand(requireNotNull(nonParticipant.id), 11_000L)),
            )

        val exception = assertFailsWith<BusinessException> { billService.registerBill(command) }

        assertEquals(BillLogErrorCode.INVALID_SHARE_PARTICIPANT, exception.errorCode)
        assertBillDataIsEmpty()
    }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val creator = saveGroupMember(group, "creator-subject", "작성자")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, creator))
        val payerParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))
        val memberParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, member))

        return MeetingContext(
            group = group,
            creator = creator,
            member = member,
            meeting = meeting,
            payerParticipant = payerParticipant,
            memberParticipant = memberParticipant,
        )
    }

    private fun createCommand(
        context: MeetingContext,
        payerMemberId: Long = requireNotNull(context.creator.id),
        totalAmount: Long = 11_000L,
        items: List<BillItemCommand> =
            listOf(
                BillItemCommand("아메리카노", 4_000L),
                BillItemCommand("프라푸치노", 7_000L),
            ),
        shares: List<BillShareCommand> =
            listOf(
                BillShareCommand(requireNotNull(context.creator.id), 4_000L),
                BillShareCommand(requireNotNull(context.member.id), 7_000L),
            ),
    ): RegisterBillCommand =
        RegisterBillCommand(
            meetingId = requireNotNull(context.meeting.id),
            creatorUserId = requireNotNull(context.creator.user.id),
            title = "아랑이 카페",
            payerMemberId = payerMemberId,
            totalAmount = totalAmount,
            splitType = BillSplitType.EQUAL_SPLIT,
            items = items,
            shares = shares,
        )

    private fun assertBillDataIsEmpty() {
        assertEquals(0L, billRepository.count())
        assertEquals(0L, billItemRepository.count())
        assertEquals(0L, billShareRepository.count())
        assertEquals(0L, settlementRequestRepository.count())
    }

    private fun createGroup(): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun createMeeting(
        group: Group,
        creator: GroupMember,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 23), LocalDate.of(2026, 9, 23)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember {
        val user = saveCompletedUser(providerUserId, nickname)
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

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
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return userRepository.saveAndFlush(user)
    }

    private data class MeetingContext(
        val group: Group,
        val creator: GroupMember,
        val member: GroupMember,
        val meeting: Meeting,
        val payerParticipant: MeetingParticipant,
        val memberParticipant: MeetingParticipant,
    )
}
