package org.com.belog.billlog.service

import jakarta.persistence.EntityManager
import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.Bill
import org.com.belog.billlog.domain.BillShare
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.BillShareRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.result.SettlementRequestAction
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
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
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    BillLogService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class BillLogServiceTest {
    @Autowired
    private lateinit var billLogService: BillLogService

    @Autowired
    private lateinit var billRepository: BillRepository

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

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    private lateinit var entityManager: EntityManager

    @MockitoBean
    private lateinit var userService: UserService

    @BeforeEach
    fun stubUserService() {
        `when`(userService.resolveDisplayNickname(anyValue())).thenAnswer { invocation ->
            requireNotNull((invocation.arguments[0] as User).nickname)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> anyValue(): T {
        any<T>()
        return null as T
    }

    @Test
    fun `총 지출 금액과 참여자별 정산 완료 및 미완료 인원을 집계한다`() {
        val context = saveMeetingContext()
        val firstBill = saveBill(context, context.firstParticipant, "첫 번째 결제", 10_000L)
        val secondBill = saveBill(context, context.firstParticipant, "두 번째 결제", 20_000L)

        saveSettlementRequest(firstBill, context.secondParticipant, 3_000L, 0, completed = true)
        saveSettlementRequest(firstBill, context.thirdParticipant, 3_000L, 1, completed = true)
        saveSettlementRequest(secondBill, context.secondParticipant, 5_000L, 0, completed = true)
        saveSettlementRequest(secondBill, context.thirdParticipant, 5_000L, 1, completed = false)
        saveSettlementRequest(secondBill, context.fourthParticipant, 5_000L, 2, completed = true)

        val result =
            billLogService.getSummary(
                meetingId = requireNotNull(context.meeting.id),
                userId = requireNotNull(context.firstMember.user.id),
            )

        assertEquals(30_000L, result.totalSpentAmount)
        assertEquals(2L, result.completedParticipantCount)
        assertEquals(1L, result.pendingParticipantCount)
    }

    @Test
    fun `만남의 모든 정산 요청에 로그인 사용자 기준 액션을 반환한다`() {
        val context = saveMeetingContext()
        val receivableBill = saveBill(context, context.firstParticipant, "받을 결제", 10_000L)
        val payableBill = saveBill(context, context.secondParticipant, "보낼 결제", 20_000L)
        val unrelatedBill = saveBill(context, context.secondParticipant, "무관한 결제", 30_000L)
        val completedBill = saveBill(context, context.firstParticipant, "완료된 결제", 40_000L)
        val receivable = saveSettlementRequest(receivableBill, context.secondParticipant, 10_000L, 0)
        val payable = saveSettlementRequest(payableBill, context.firstParticipant, 20_000L, 0)
        val unrelated = saveSettlementRequest(unrelatedBill, context.thirdParticipant, 30_000L, 0)
        val completed = saveSettlementRequest(completedBill, context.fourthParticipant, 40_000L, 0, completed = true)

        val result =
            billLogService.getSettlementRequests(
                meetingId = requireNotNull(context.meeting.id),
                userId = requireNotNull(context.firstMember.user.id),
                cursor = null,
                size = 10,
            )
        val itemsById = result.items.associateBy { item -> item.settlementRequestId }

        assertEquals(setOf(receivable.id, payable.id, unrelated.id, completed.id), itemsById.keys)
        assertEquals(SettlementRequestAction.SEND_REMINDER, itemsById.getValue(requireNotNull(receivable.id)).action)
        assertEquals(SettlementRequestAction.MARK_COMPLETE, itemsById.getValue(requireNotNull(payable.id)).action)
        assertEquals(SettlementRequestAction.NONE, itemsById.getValue(requireNotNull(unrelated.id)).action)
        assertEquals(SettlementRequestAction.NONE, itemsById.getValue(requireNotNull(completed.id)).action)
        assertTrue(itemsById.getValue(requireNotNull(payable.id)).sender.isMe)
        assertTrue(itemsById.getValue(requireNotNull(receivable.id)).receiver.isMe)
        assertFalse(itemsById.getValue(requireNotNull(unrelated.id)).sender.isMe)
        assertFalse(itemsById.getValue(requireNotNull(unrelated.id)).receiver.isMe)
    }

    @Test
    fun `진행 중인 정산을 먼저 정렬하고 상태 경계에서도 커서로 중복 없이 조회한다`() {
        val context = saveMeetingContext()
        val bills =
            (1..5).map { number ->
                saveBill(context, context.firstParticipant, "결제 $number", number * 1_000L)
            }
        val requests =
            listOf(
                saveSettlementRequest(bills[0], context.secondParticipant, 1_000L, 0, completed = true),
                saveSettlementRequest(bills[1], context.secondParticipant, 2_000L, 0),
                saveSettlementRequest(bills[2], context.secondParticipant, 3_000L, 0, completed = true),
                saveSettlementRequest(bills[3], context.secondParticipant, 4_000L, 0),
                saveSettlementRequest(bills[4], context.secondParticipant, 5_000L, 0),
            )
        val expectedIds =
            listOf(requests[4], requests[3], requests[1], requests[2], requests[0])
                .map { request -> requireNotNull(request.id) }

        val firstPage = getSettlementRequestPage(context, cursor = null, size = 2)
        val secondPage = getSettlementRequestPage(context, cursor = firstPage.nextCursor, size = 2)
        val thirdPage = getSettlementRequestPage(context, cursor = secondPage.nextCursor, size = 2)
        val actualIds = (firstPage.items + secondPage.items + thirdPage.items).map { item -> item.settlementRequestId }

        assertEquals(expectedIds, actualIds)
        assertEquals(
            listOf(
                SettlementRequestStatus.PENDING,
                SettlementRequestStatus.PENDING,
                SettlementRequestStatus.PENDING,
                SettlementRequestStatus.COMPLETED,
                SettlementRequestStatus.COMPLETED,
            ),
            firstPage.items.map { item -> item.status } +
                secondPage.items.map { item -> item.status } +
                thirdPage.items.map { item -> item.status },
        )
        assertTrue(firstPage.hasNext)
        assertTrue(secondPage.hasNext)
        assertFalse(thirdPage.hasNext)
        assertNull(thirdPage.nextCursor)
    }

    @Test
    fun `정산 현황 커서가 올바르지 않으면 입력 오류를 반환한다`() {
        val context = saveMeetingContext()

        val exception =
            assertFailsWith<BusinessException> {
                getSettlementRequestPage(context, cursor = "invalid-cursor", size = 10)
            }

        assertEquals(BillLogErrorCode.INVALID_SETTLEMENT_REQUEST_CURSOR, exception.errorCode)
    }

    @Test
    fun `결제 내역을 날짜별로 묶고 일별 총액과 일차를 계산한다`() {
        val context = saveMeetingContext()
        val first = saveBill(context, context.firstParticipant, "카페", 7_000L)
        val second = saveBill(context, context.secondParticipant, "저녁", 8_000L)
        val third = saveBill(context, context.firstParticipant, "점심", 11_000L)
        val beforeMeeting = saveBill(context, context.firstParticipant, "사전 결제", 5_000L)
        updateBillCreatedAt(first, Instant.parse("2026-09-25T01:00:00Z"))
        updateBillCreatedAt(second, Instant.parse("2026-09-25T02:00:00Z"))
        updateBillCreatedAt(third, Instant.parse("2026-09-24T01:00:00Z"))
        updateBillCreatedAt(beforeMeeting, Instant.parse("2026-09-22T01:00:00Z"))
        entityManager.clear()

        val result =
            billLogService.getBills(
                meetingId = requireNotNull(context.meeting.id),
                userId = requireNotNull(context.firstMember.user.id),
                cursorDate = null,
                size = 10,
            )

        assertEquals(
            listOf(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 24), LocalDate.of(2026, 9, 22)),
            result.days.map { day -> day.paymentDate },
        )
        assertEquals(listOf(3, 2, -1), result.days.map { day -> day.dayNumber })
        assertEquals(listOf(15_000L, 11_000L, 5_000L), result.days.map { day -> day.dailyTotalAmount })
        assertEquals(
            listOf("저녁", "카페"),
            result.days
                .first()
                .bills
                .map { bill -> bill.title },
        )
        assertEquals(
            listOf("두 번째", "첫 번째"),
            result.days
                .first()
                .bills
                .map { bill -> bill.payerNickname },
        )
    }

    @Test
    fun `동일 날짜의 결제 내역을 분리하지 않고 날짜 단위로 페이지네이션한다`() {
        val context = saveMeetingContext()
        val first = saveBill(context, context.firstParticipant, "첫 결제", 7_000L)
        val second = saveBill(context, context.secondParticipant, "두 번째 결제", 8_000L)
        val third = saveBill(context, context.firstParticipant, "다음 날짜 결제", 11_000L)
        updateBillCreatedAt(first, Instant.parse("2026-09-25T01:00:00Z"))
        updateBillCreatedAt(second, Instant.parse("2026-09-25T02:00:00Z"))
        updateBillCreatedAt(third, Instant.parse("2026-09-24T01:00:00Z"))
        entityManager.clear()

        val firstPage = getBillPage(context, cursorDate = null, size = 1)
        val secondPage = getBillPage(context, cursorDate = firstPage.nextCursorDate, size = 1)

        assertEquals(LocalDate.of(2026, 9, 25), firstPage.days.single().paymentDate)
        assertEquals(
            2,
            firstPage.days
                .single()
                .bills
                .size,
        )
        assertEquals(LocalDate.of(2026, 9, 25), firstPage.nextCursorDate)
        assertTrue(firstPage.hasNext)
        assertEquals(LocalDate.of(2026, 9, 24), secondPage.days.single().paymentDate)
        assertEquals(
            1,
            secondPage.days
                .single()
                .bills
                .size,
        )
        assertNull(secondPage.nextCursorDate)
        assertFalse(secondPage.hasNext)
    }

    private fun getSettlementRequestPage(
        context: MeetingContext,
        cursor: String?,
        size: Int,
    ) = billLogService.getSettlementRequests(
        meetingId = requireNotNull(context.meeting.id),
        userId = requireNotNull(context.firstMember.user.id),
        cursor = cursor,
        size = size,
    )

    private fun getBillPage(
        context: MeetingContext,
        cursorDate: LocalDate?,
        size: Int,
    ) = billLogService.getBills(
        meetingId = requireNotNull(context.meeting.id),
        userId = requireNotNull(context.firstMember.user.id),
        cursorDate = cursorDate,
        size = size,
    )

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val firstMember = saveGroupMember(group, "first-subject", "첫 번째")
        val secondMember = saveGroupMember(group, "second-subject", "두 번째")
        val thirdMember = saveGroupMember(group, "third-subject", "세 번째")
        val fourthMember = saveGroupMember(group, "fourth-subject", "네 번째")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, firstMember))

        return MeetingContext(
            group = group,
            meeting = meeting,
            firstMember = firstMember,
            firstParticipant = saveParticipant(meeting, firstMember),
            secondParticipant = saveParticipant(meeting, secondMember),
            thirdParticipant = saveParticipant(meeting, thirdMember),
            fourthParticipant = saveParticipant(meeting, fourthMember),
        )
    }

    private fun saveParticipant(
        meeting: Meeting,
        member: GroupMember,
    ): MeetingParticipant = meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, member))

    private fun saveBill(
        context: MeetingContext,
        payer: MeetingParticipant,
        title: String,
        totalAmount: Long,
    ): Bill =
        billRepository.saveAndFlush(
            Bill.create(
                meeting = context.meeting,
                creator = context.firstMember,
                payer = payer,
                title = title,
                totalAmount = totalAmount,
                splitType = BillSplitType.EQUAL_SPLIT,
            ),
        )

    private fun saveSettlementRequest(
        bill: Bill,
        participant: MeetingParticipant,
        amount: Long,
        allocationOrder: Int,
        completed: Boolean = false,
    ): SettlementRequest {
        val share =
            billShareRepository.saveAndFlush(
                BillShare.create(
                    bill = bill,
                    participant = participant,
                    amount = amount,
                    allocationOrder = allocationOrder,
                ),
            )
        val request = SettlementRequest.create(share)
        if (completed) {
            request.complete(Instant.parse("2026-09-30T00:00:00Z"))
        }
        return settlementRequestRepository.saveAndFlush(request)
    }

    private fun updateBillCreatedAt(
        bill: Bill,
        createdAt: Instant,
    ) {
        billRepository.flush()
        jdbcTemplate.update(
            "UPDATE bill_log_bills SET created_at = ? WHERE id = ?",
            Timestamp.from(createdAt),
            requireNotNull(bill.id),
        )
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
            dateRange = MeetingDateRange(MEETING_START_DATE, LocalDate.of(2026, 9, 26)),
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
        val meeting: Meeting,
        val firstMember: GroupMember,
        val firstParticipant: MeetingParticipant,
        val secondParticipant: MeetingParticipant,
        val thirdParticipant: MeetingParticipant,
        val fourthParticipant: MeetingParticipant,
    )

    companion object {
        private val MEETING_START_DATE: LocalDate = LocalDate.of(2026, 9, 23)
    }
}
