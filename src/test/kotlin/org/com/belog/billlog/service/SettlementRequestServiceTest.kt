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
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.service.NotificationService
import org.com.belog.notification.service.command.CreateNotificationCommand
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
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Primary
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.sql.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

@DataJpaTest
@ActiveProfiles("test")
@Import(
    SettlementRequestService::class,
    SettlementRequestServiceTest.FixedClockConfig::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class SettlementRequestServiceTest {
    @Autowired
    private lateinit var settlementRequestService: SettlementRequestService

    @Autowired
    private lateinit var settlementRequestRepository: SettlementRequestRepository

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var billShareRepository: BillShareRepository

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
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @MockitoBean
    private lateinit var notificationService: NotificationService

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
    fun `정산 요청 대상자가 요청을 완료하면 완료 상태와 완료 시각이 저장된다`() {
        val context = saveSettlementContext()

        settlementRequestService.complete(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.settlementTargetUserId,
        )
        flushAndClear()

        val settlementRequest = settlementRequestRepository.findById(context.settlementRequestId).orElseThrow()
        assertEquals(SettlementRequestStatus.COMPLETED, settlementRequest.status)
        assertEquals(FIXED_INSTANT, settlementRequest.completedAt)
    }

    @Test
    fun `정산 요청 대상자가 요청을 완료하면 결제자에게 정산 완료 알림이 저장된다`() {
        val context = saveSettlementContext()

        settlementRequestService.complete(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.settlementTargetUserId,
        )

        val expectedCommand =
            CreateNotificationCommand(
                recipientUserId = context.payerUserId,
                actorUserId = context.settlementTargetUserId,
                type = NotificationType.SETTLEMENT_COMPLETED,
                message = "정산이 완료됐어요",
                targetId = context.meetingId,
                deduplicationKey = "SETTLEMENT_COMPLETED:${context.settlementRequestId}",
            )
        assertEquals(listOf(expectedCommand), requestedNotificationCommands())
    }

    @Test
    fun `정산 요청 대상자가 아닌 결제자는 요청을 완료할 수 없다`() {
        val context = saveSettlementContext()

        val exception =
            assertFailsWith<BusinessException> {
                settlementRequestService.complete(
                    settlementRequestId = context.settlementRequestId,
                    requesterUserId = context.payerUserId,
                )
            }
        flushAndClear()

        assertEquals(BillLogErrorCode.SETTLEMENT_REQUEST_ACCESS_DENIED, exception.errorCode)
        assertSettlementIsPending(context.settlementRequestId)
    }

    @Test
    fun `존재하지 않는 정산 요청은 완료할 수 없다`() {
        val context = saveSettlementContext()

        val exception =
            assertFailsWith<BusinessException> {
                settlementRequestService.complete(
                    settlementRequestId = Long.MAX_VALUE,
                    requesterUserId = context.settlementTargetUserId,
                )
            }
        flushAndClear()

        assertEquals(BillLogErrorCode.SETTLEMENT_REQUEST_NOT_FOUND, exception.errorCode)
        assertSettlementIsPending(context.settlementRequestId)
    }

    @Test
    fun `이미 완료된 정산 요청을 다시 완료해도 최초 완료 시각이 유지된다`() {
        val context = saveSettlementContext()

        settlementRequestService.complete(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.settlementTargetUserId,
        )
        flushAndClear()
        settlementRequestService.complete(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.settlementTargetUserId,
        )
        flushAndClear()

        val settlementRequest = settlementRequestRepository.findById(context.settlementRequestId).orElseThrow()
        assertEquals(SettlementRequestStatus.COMPLETED, settlementRequest.status)
        assertEquals(FIXED_INSTANT, settlementRequest.completedAt)
        assertEquals(1, requestedNotificationCommands().size)
    }

    @Test
    fun `결제자가 정산 리마인드를 보내면 정산 대상자에게 리마인드 알림이 저장된다`() {
        val context = saveSettlementContext()

        settlementRequestService.remind(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.payerUserId,
        )
        flushAndClear()

        val expectedCommand =
            CreateNotificationCommand(
                recipientUserId = context.settlementTargetUserId,
                actorUserId = context.payerUserId,
                type = NotificationType.SETTLEMENT_REMINDER,
                message = "결제자 님이 정산을 다시 요청했어요",
                targetId = context.meetingId,
                deduplicationKey = "SETTLEMENT_REMINDER:${context.settlementRequestId}:${FIXED_INSTANT.toEpochMilli()}",
            )
        assertEquals(listOf(expectedCommand), requestedNotificationCommands())
        assertEquals(
            FIXED_INSTANT,
            settlementRequestRepository.findById(context.settlementRequestId).orElseThrow().lastRemindedAt,
        )
    }

    @Test
    fun `결제자가 아닌 사용자는 정산 리마인드를 보낼 수 없다`() {
        val context = saveSettlementContext()

        val exception =
            assertFailsWith<BusinessException> {
                settlementRequestService.remind(
                    settlementRequestId = context.settlementRequestId,
                    requesterUserId = context.settlementTargetUserId,
                )
            }

        assertEquals(BillLogErrorCode.SETTLEMENT_REMINDER_ACCESS_DENIED, exception.errorCode)
        assertEquals(emptyList(), requestedNotificationCommands())
    }

    @Test
    fun `완료된 정산 요청에는 리마인드를 보낼 수 없다`() {
        val context = saveSettlementContext()
        settlementRequestService.complete(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.settlementTargetUserId,
        )
        flushAndClear()

        val exception =
            assertFailsWith<BusinessException> {
                settlementRequestService.remind(
                    settlementRequestId = context.settlementRequestId,
                    requesterUserId = context.payerUserId,
                )
            }

        assertEquals(BillLogErrorCode.SETTLEMENT_REQUEST_ALREADY_COMPLETED, exception.errorCode)
        assertEquals(emptyList(), requestedNotificationCommands(NotificationType.SETTLEMENT_REMINDER))
    }

    @Test
    fun `마지막 리마인드 후 1분이 지나지 않으면 정산 리마인드를 다시 보낼 수 없다`() {
        val context = saveSettlementContext()
        updateLastRemindedAt(context.settlementRequestId, FIXED_INSTANT.minusSeconds(59))

        val exception =
            assertFailsWith<BusinessException> {
                settlementRequestService.remind(
                    settlementRequestId = context.settlementRequestId,
                    requesterUserId = context.payerUserId,
                )
            }

        assertEquals(BillLogErrorCode.SETTLEMENT_REMINDER_TOO_FREQUENT, exception.errorCode)
        assertEquals(emptyList(), requestedNotificationCommands())
    }

    @Test
    fun `마지막 리마인드 후 1분이 지나면 정산 리마인드를 다시 보낼 수 있다`() {
        val context = saveSettlementContext()
        updateLastRemindedAt(context.settlementRequestId, FIXED_INSTANT.minusSeconds(60))

        settlementRequestService.remind(
            settlementRequestId = context.settlementRequestId,
            requesterUserId = context.payerUserId,
        )
        flushAndClear()

        assertEquals(1, requestedNotificationCommands().size)
        assertEquals(
            FIXED_INSTANT,
            settlementRequestRepository.findById(context.settlementRequestId).orElseThrow().lastRemindedAt,
        )
    }

    private fun updateLastRemindedAt(
        settlementRequestId: Long,
        lastRemindedAt: Instant,
    ) {
        jdbcTemplate.update(
            "UPDATE bill_log_settlement_requests SET last_reminded_at = ? WHERE id = ?",
            Timestamp.from(lastRemindedAt),
            settlementRequestId,
        )
        entityManager.clear()
    }

    private fun requestedNotificationCommands(type: NotificationType? = null): List<CreateNotificationCommand> =
        mockingDetails(notificationService)
            .invocations
            .filter { invocation -> invocation.method.name == "create" }
            .map { invocation -> invocation.arguments.single() as CreateNotificationCommand }
            .filter { command -> type == null || command.type == type }

    private fun saveSettlementContext(): SettlementContext {
        val group = groupRepository.save(createGroup())
        val payerMember = saveGroupMember(group, "payer-subject", "결제자")
        val settlementTargetMember = saveGroupMember(group, "settlement-target-subject", "정산자")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, payerMember, "광주 여행"))
        val payerParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, payerMember))
        val settlementTargetParticipant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, settlementTargetMember))
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
                BillShare.create(
                    bill = bill,
                    participant = settlementTargetParticipant,
                    amount = 10_000L,
                    allocationOrder = 0,
                ),
            )
        val settlementRequest = settlementRequestRepository.saveAndFlush(SettlementRequest.create(share))

        return SettlementContext(
            group = group,
            payerMember = payerMember,
            meetingId = requireNotNull(meeting.id),
            settlementRequestId = requireNotNull(settlementRequest.id),
            payerUserId = requireNotNull(payerMember.user.id),
            settlementTargetUserId = requireNotNull(settlementTargetMember.user.id),
        )
    }

    private fun assertSettlementIsPending(settlementRequestId: Long) {
        val settlementRequest = settlementRequestRepository.findById(settlementRequestId).orElseThrow()
        assertEquals(SettlementRequestStatus.PENDING, settlementRequest.status)
        assertNull(settlementRequest.completedAt)
    }

    private fun flushAndClear() {
        entityManager.flush()
        entityManager.clear()
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
        name: String,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = name,
            location = null,
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 29), LocalDate.of(2026, 9, 29)),
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

    private data class SettlementContext(
        val group: Group,
        val payerMember: GroupMember,
        val meetingId: Long,
        val settlementRequestId: Long,
        val payerUserId: Long,
        val settlementTargetUserId: Long,
    )

    @TestConfiguration
    class FixedClockConfig {
        @Bean
        @Primary
        fun fixedClock(): Clock = Clock.fixed(FIXED_INSTANT, ZoneOffset.UTC)
    }

    companion object {
        private val FIXED_INSTANT: Instant = Instant.parse("2026-09-28T00:00:00Z")
    }
}
