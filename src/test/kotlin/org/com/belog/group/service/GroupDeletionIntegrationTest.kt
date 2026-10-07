package org.com.belog.group.service

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
import org.com.belog.meeting.service.MeetingService
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
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(MeetingServiceTest.FixedClockConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class GroupDeletionIntegrationTest {
    @Autowired
    private lateinit var groupService: GroupService

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
    fun `OWNER가 그룹을 삭제하면 하위 만남도 삭제되고 내 그룹 목록에서 제외된다`() {
        val context = saveGroupContext()

        groupService.deleteGroup(context.groupId, context.ownerUserId)

        val deletedGroup = groupRepository.findById(context.groupId).orElseThrow()
        val deletedMeeting = meetingRepository.findById(context.meetingId).orElseThrow()
        assertNotNull(deletedGroup.deletedAt)
        assertEquals(deletedGroup.deletedAt, deletedMeeting.deletedAt)
        assertTrue(groupService.getMyGroups(context.ownerUserId, null, 10).items.isEmpty())
    }

    @Test
    fun `OWNER가 아닌 그룹 멤버는 그룹을 삭제할 수 없다`() {
        val context = saveGroupContext()

        val exception =
            assertFailsWith<BusinessException> {
                groupService.deleteGroup(context.groupId, context.memberUserId)
            }

        assertEquals(GroupErrorCode.GROUP_DELETE_OWNER_REQUIRED, exception.errorCode)
        assertNull(groupRepository.findById(context.groupId).orElseThrow().deletedAt)
    }

    @Test
    fun `그룹의 만남에 정산이 완료되지 않은 요청이 있으면 그룹을 삭제할 수 없다`() {
        val context = saveGroupContext()
        billService.registerBill(createBillCommand(context))

        val exception =
            assertFailsWith<BusinessException> {
                groupService.deleteGroup(context.groupId, context.ownerUserId)
            }

        assertEquals(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS, exception.errorCode)
        assertNull(groupRepository.findById(context.groupId).orElseThrow().deletedAt)
        assertNull(meetingRepository.findById(context.meetingId).orElseThrow().deletedAt)
    }

    @Test
    fun `그룹 삭제와 결제 등록이 동시에 요청되면 하나만 성공하고 삭제된 그룹에 미정산 요청이 남지 않는다`() {
        val context = saveGroupContext()

        val results =
            runConcurrently(
                Callable { groupService.deleteGroup(context.groupId, context.ownerUserId) },
                Callable { billService.registerBill(createBillCommand(context)) },
            )

        val groupDeleted = groupRepository.findById(context.groupId).orElseThrow().deletedAt != null
        val pendingExists =
            settlementRequestRepository.findAll().any { request -> request.status == SettlementRequestStatus.PENDING }
        assertEquals(1, results.count { result -> result == null })
        assertFalse(groupDeleted && pendingExists)
    }

    @Test
    fun `그룹 삭제와 만남 생성이 동시에 요청되어도 삭제된 그룹에 활성 만남이 남지 않는다`() {
        val context = saveGroupContext()

        runConcurrently(
            Callable { groupService.deleteGroup(context.groupId, context.ownerUserId) },
            Callable {
                meetingService.createFixedMeeting(
                    groupId = context.groupId,
                    creatorUserId = context.memberUserId,
                    name = "부산 여행",
                    location = null,
                    participantMemberIds = emptyList(),
                    startDate = LocalDate.of(2026, 10, 1),
                    endDate = LocalDate.of(2026, 10, 2),
                )
            },
        )

        assertNotNull(groupRepository.findById(context.groupId).orElseThrow().deletedAt)
        assertTrue(
            meetingRepository
                .findAll()
                .filter { meeting -> meeting.group.id == context.groupId }
                .all { meeting -> meeting.deletedAt != null },
        )
    }

    private fun runConcurrently(vararg requests: Callable<*>): List<ErrorCode?> {
        val executor = Executors.newFixedThreadPool(requests.size)
        val startSignal = CountDownLatch(1)

        try {
            val futures =
                requests.map { request ->
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
            return futures.map { future -> future.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }

    private fun saveGroupContext(): GroupContext {
        val group = groupRepository.save(Group.create("주말 여행 모임", null, InviteCode.create("AB12CD")))
        val owner = groupMemberRepository.saveAndFlush(GroupMember.createOwner(group, saveCompletedUser("owner", "방장")))
        val member = groupMemberRepository.saveAndFlush(GroupMember.createMember(group, saveCompletedUser("member", "멤버")))
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
            listOf(MeetingParticipant.create(meeting, owner), MeetingParticipant.create(meeting, member)),
        )

        return GroupContext(
            groupId = requireNotNull(group.id),
            meetingId = requireNotNull(meeting.id),
            ownerUserId = requireNotNull(owner.user.id),
            ownerMemberId = requireNotNull(owner.id),
            memberUserId = requireNotNull(member.user.id),
            memberId = requireNotNull(member.id),
        )
    }

    private fun createBillCommand(context: GroupContext): RegisterBillCommand =
        RegisterBillCommand(
            meetingId = context.meetingId,
            creatorUserId = context.ownerUserId,
            title = "저녁 식사",
            payerMemberId = context.ownerMemberId,
            totalAmount = 20_000L,
            splitType = BillSplitType.EQUAL_SPLIT,
            items = listOf(BillItemCommand(name = "식사", amount = 20_000L)),
            shares =
                listOf(
                    BillShareCommand(participantMemberId = context.ownerMemberId, amount = 10_000L),
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

    private data class GroupContext(
        val groupId: Long,
        val meetingId: Long,
        val ownerUserId: Long,
        val ownerMemberId: Long,
        val memberUserId: Long,
        val memberId: Long,
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
