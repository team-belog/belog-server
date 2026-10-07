package org.com.belog.meeting.service

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.domain.SettlementRequestStatus
import org.com.belog.billlog.repository.BillItemRepository
import org.com.belog.billlog.repository.BillRepository
import org.com.belog.billlog.repository.BillShareRepository
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.BillService
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.global.error.BusinessException
import org.com.belog.global.response.code.ErrorCode
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

@SpringBootTest
@ActiveProfiles("test")
@Import(MeetingServiceTest.FixedClockConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MeetingDeletionIntegrationTest {
    @Autowired
    private lateinit var meetingService: MeetingService

    @Autowired
    private lateinit var billService: BillService

    @Autowired
    private lateinit var settlementRequestRepository: SettlementRequestRepository

    @Autowired
    private lateinit var billShareRepository: BillShareRepository

    @Autowired
    private lateinit var billItemRepository: BillItemRepository

    @Autowired
    private lateinit var billRepository: BillRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        settlementRequestRepository.deleteAllInBatch()
        billShareRepository.deleteAllInBatch()
        billItemRepository.deleteAllInBatch()
        billRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `정산이 완료되지 않은 만남은 삭제할 수 없다`() {
        val context = saveMeetingContext()
        billService.registerBill(createBillCommand(context))

        val exception =
            assertFailsWith<BusinessException> {
                meetingService.deleteMeeting(context.meetingId, context.creatorUserId)
            }

        assertEquals(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS, exception.errorCode)
        assertNull(meetingRepository.findById(context.meetingId).orElseThrow().deletedAt)
    }

    @Test
    fun `만남 삭제와 결제 등록이 동시에 요청되면 하나만 성공하고 삭제된 만남에 미정산 요청이 남지 않는다`() {
        val context = saveMeetingContext()
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val requests =
                listOf(
                    Callable { meetingService.deleteMeeting(context.meetingId, context.creatorUserId) },
                    Callable { billService.registerBill(createBillCommand(context)) },
                ).map { request ->
                    executor.submit<ErrorCode?> {
                        startSignal.await()
                        try {
                            request.call()
                            null
                        } catch (exception: BusinessException) {
                            exception.errorCode
                        }
                    }
                }

            startSignal.countDown()
            val results = requests.map { request -> request.get(10, TimeUnit.SECONDS) }
            val meetingDeleted = meetingRepository.findById(context.meetingId).orElseThrow().deletedAt != null
            val pendingExists =
                settlementRequestRepository.findAll().any { request -> request.status == SettlementRequestStatus.PENDING }

            assertEquals(1, results.count { result -> result == null })
            assertFalse(meetingDeleted && pendingExists)
        } finally {
            executor.shutdownNow()
        }
    }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(Group.create("주말 여행 모임", null, InviteCode.create("AB12CD")))
        val creator = groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, saveCompletedUser("creator", "생성자")))
        val member = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, saveCompletedUser("member", "멤버")))
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createFixed(
                    group = group,
                    creator = creator,
                    name = "광주 여행",
                    location = null,
                    dateRange = MeetingDateRange(LocalDate.of(2026, 9, 25), LocalDate.of(2026, 9, 26)),
                    confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
                    currentDate = LocalDate.of(2026, 9, 20),
                ),
            )
        meetingParticipantRepository.saveAllAndFlush(
            listOf(MeetingParticipant.create(meeting, creator), MeetingParticipant.create(meeting, member)),
        )

        return MeetingContext(
            meetingId = requireNotNull(meeting.id),
            creatorUserId = requireNotNull(creator.user.id),
            creatorMemberId = requireNotNull(creator.id),
            memberId = requireNotNull(member.id),
        )
    }

    private fun createBillCommand(context: MeetingContext): RegisterBillCommand =
        RegisterBillCommand(
            meetingId = context.meetingId,
            creatorUserId = context.creatorUserId,
            title = "저녁 식사",
            payerMemberId = context.creatorMemberId,
            totalAmount = 20_000L,
            splitType = BillSplitType.EQUAL_SPLIT,
            items = listOf(BillItemCommand(name = "식사", amount = 20_000L)),
            shares =
                listOf(
                    BillShareCommand(participantMemberId = context.creatorMemberId, amount = 10_000L),
                    BillShareCommand(participantMemberId = context.memberId, amount = 10_000L),
                ),
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

    private data class MeetingContext(
        val meetingId: Long,
        val creatorUserId: Long,
        val creatorMemberId: Long,
        val memberId: Long,
    )

    companion object {
        private const val CONCURRENT_REQUEST_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
