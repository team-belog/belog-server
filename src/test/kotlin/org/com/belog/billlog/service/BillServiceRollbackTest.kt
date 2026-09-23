package org.com.belog.billlog.service

import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.domain.SettlementRequest
import org.com.belog.billlog.repository.SettlementRequestRepository
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.global.config.JpaAuditingConfig
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
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
class BillServiceRollbackTest {
    @Autowired
    private lateinit var billService: BillService

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

    @MockitoBean
    private lateinit var settlementRequestRepository: SettlementRequestRepository

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `정산 요청 저장 중 실패하면 결제 내역 전체가 롤백된다`() {
        val group = groupRepository.save(createGroup())
        val creator = saveGroupMember(group, "creator-subject", "작성자")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, creator))
        meetingParticipantRepository.saveAllAndFlush(
            listOf(
                MeetingParticipant.create(meeting, creator),
                MeetingParticipant.create(meeting, member),
            ),
        )
        doThrow(IllegalStateException("정산 요청 저장 실패"))
            .`when`(settlementRequestRepository)
            .saveAll(anyList<SettlementRequest>())
        val command =
            RegisterBillCommand(
                meetingId = requireNotNull(meeting.id),
                creatorUserId = requireNotNull(creator.user.id),
                title = "아랑이 카페",
                payerMemberId = requireNotNull(creator.id),
                totalAmount = 11_000L,
                splitType = BillSplitType.EQUAL_SPLIT,
                items =
                    listOf(
                        BillItemCommand("아메리카노", 4_000L),
                        BillItemCommand("프라푸치노", 7_000L),
                    ),
                shares =
                    listOf(
                        BillShareCommand(requireNotNull(creator.id), 4_000L),
                        BillShareCommand(requireNotNull(member.id), 7_000L),
                    ),
            )

        assertFailsWith<IllegalStateException> { billService.registerBill(command) }

        assertEquals(0L, countRows("bill_log_bills"))
        assertEquals(0L, countRows("bill_log_bill_items"))
        assertEquals(0L, countRows("bill_log_bill_shares"))
        assertEquals(0L, countRows("bill_log_settlement_requests"))
    }

    private fun countRows(tableName: String): Long =
        requireNotNull(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $tableName", Long::class.java))

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
}
