package org.com.belog.group.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
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
    GroupReminderService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class GroupReminderServiceTest {
    @Autowired
    private lateinit var groupReminderService: GroupReminderService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var notificationService: NotificationService

    @Test
    fun `마지막 종료 여정 후 60일이 지난 그룹의 탈퇴하지 않은 멤버에게 마지막 여정 기준 리마인드 알림을 요청한다`() {
        val context = saveGroupContext()

        groupReminderService.remindInactiveGroup(context.groupId, LAST_END_DATE.plusDays(60))

        val expectedCommands =
            context.activeUserIds.map { recipientUserId ->
                CreateNotificationCommand(
                    recipientUserId = recipientUserId,
                    actorUserId = null,
                    type = NotificationType.GROUP_INACTIVE_60_DAYS,
                    message = "마지막 만남 이후 60일, 슬슬 다시 만나볼 때 아닌가요?",
                    targetId = context.groupId,
                    deduplicationKey =
                        "${NotificationType.GROUP_INACTIVE_60_DAYS}:${context.groupId}:" +
                            "${context.lastMeetingId}:$recipientUserId",
                )
            }
        assertEquals(listOf(expectedCommands), requestedNotificationCommandBatches())
    }

    @Test
    fun `마지막 종료 여정 후 60일이 지나지 않았으면 리마인드 알림을 요청하지 않는다`() {
        val context = saveGroupContext()

        groupReminderService.remindInactiveGroup(context.groupId, LAST_END_DATE.plusDays(59))

        assertTrue(requestedNotificationCommandBatches().isEmpty())
    }

    @Test
    fun `일정 조율 중인 만남이 있으면 리마인드 알림을 요청하지 않는다`() {
        val context = saveGroupContext()
        meetingRepository.saveAndFlush(
            Meeting.createPoll(group = context.group, creator = context.owner, name = "다음 여행", location = null),
        )

        groupReminderService.remindInactiveGroup(context.groupId, LAST_END_DATE.plusDays(60))

        assertTrue(requestedNotificationCommandBatches().isEmpty())
    }

    private fun requestedNotificationCommandBatches(): List<List<*>> =
        mockingDetails(notificationService)
            .invocations
            .filter { invocation -> invocation.method.name == "createAll" }
            .map { invocation -> invocation.arguments.single() as List<*> }

    private fun saveGroupContext(): GroupContext {
        val group = groupRepository.save(createGroup())
        val owner = saveGroupMember(group, "owner-subject", "방장")
        val member = saveGroupMember(group, "member-subject", "멤버")
        val withdrawnMember = saveGroupMember(group, "withdrawn-subject", "탈퇴자")
        withdrawnMember.withdraw(Instant.parse("2026-09-20T00:00:00Z"))
        groupMemberRepository.saveAndFlush(withdrawnMember)
        meetingRepository.save(createMeeting(group, owner, LAST_END_DATE.minusDays(3), LAST_END_DATE.minusDays(2)))
        val lastMeeting = meetingRepository.saveAndFlush(createMeeting(group, owner, LAST_END_DATE, LAST_END_DATE))

        return GroupContext(
            group = group,
            owner = owner,
            groupId = requireNotNull(group.id),
            lastMeetingId = requireNotNull(lastMeeting.id),
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
        startDate: LocalDate,
        endDate: LocalDate,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            dateRange = MeetingDateRange(startDate, endDate),
            confirmedAt = Instant.parse("2026-09-15T00:00:00Z"),
            currentDate = startDate,
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

    private data class GroupContext(
        val group: Group,
        val owner: GroupMember,
        val groupId: Long,
        val lastMeetingId: Long,
        val activeUserIds: List<Long>,
    )

    companion object {
        private val LAST_END_DATE: LocalDate = LocalDate.of(2026, 9, 25)
    }
}
