package org.com.belog.prelog.service

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
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    PreLogReminderService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class PreLogReminderServiceTest {
    @Autowired
    private lateinit var preLogReminderService: PreLogReminderService

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

    @MockitoBean
    private lateinit var notificationService: NotificationService

    @Test
    fun `시작 2일 전인 만남의 탈퇴하지 않은 참여자에게 Pre-log 작성 리마인드 알림을 요청한다`() {
        val context = saveMeetingContext()

        preLogReminderService.remindPreLogBeforeMeetingStart(context.meetingId, CURRENT_DATE)

        val expectedCommands =
            context.activeUserIds.map { recipientUserId ->
                CreateNotificationCommand(
                    recipientUserId = recipientUserId,
                    actorUserId = null,
                    type = NotificationType.PRE_LOG_D2_REMINDER,
                    message = "이틀 남았어요, Pre-log를 마저 작성해볼까요?",
                    targetId = context.meetingId,
                    deduplicationKey =
                        "${NotificationType.PRE_LOG_D2_REMINDER}:${context.meetingId}:$START_DATE:$recipientUserId",
                )
            }
        assertEquals(listOf(expectedCommands), requestedNotificationCommandBatches())
    }

    @Test
    fun `만남 시작일이 2일 뒤가 아니면 Pre-log 작성 리마인드 알림을 요청하지 않는다`() {
        val context = saveMeetingContext()

        preLogReminderService.remindPreLogBeforeMeetingStart(context.meetingId, CURRENT_DATE.plusDays(1))

        assertTrue(requestedNotificationCommandBatches().isEmpty())
    }

    private fun requestedNotificationCommandBatches(): List<List<*>> =
        mockingDetails(notificationService)
            .invocations
            .filter { invocation -> invocation.method.name == "createAll" }
            .map { invocation -> invocation.arguments.single() as List<*> }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val owner = saveGroupMember(group, "owner-subject", "방장")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val withdrawnMember = saveGroupMember(group, "withdrawn-subject", "탈퇴자")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, owner))
        meetingParticipantRepository.saveAllAndFlush(
            listOf(owner, member, withdrawnMember).map { groupMember -> MeetingParticipant.create(meeting, groupMember) },
        )
        withdrawnMember.withdraw(Instant.parse("2026-09-20T00:00:00Z"))
        groupMemberRepository.saveAndFlush(withdrawnMember)

        return MeetingContext(
            meetingId = requireNotNull(meeting.id),
            activeUserIds = listOf(owner, member).map { groupMember -> requireNotNull(groupMember.user.id) },
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
            dateRange = MeetingDateRange(START_DATE, START_DATE.plusDays(1)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = CURRENT_DATE,
        )

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember {
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
        userRepository.saveAndFlush(user)
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

    private data class MeetingContext(
        val meetingId: Long,
        val activeUserIds: List<Long>,
    )

    companion object {
        private val CURRENT_DATE: LocalDate = LocalDate.of(2026, 9, 21)
        private val START_DATE: LocalDate = CURRENT_DATE.plusDays(2)
    }
}
