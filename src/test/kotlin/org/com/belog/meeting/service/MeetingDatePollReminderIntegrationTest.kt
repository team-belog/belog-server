package org.com.belog.meeting.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.domain.MeetingScheduleResponse
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
import org.com.belog.notification.domain.NotificationType
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Clock
import java.time.Instant
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MeetingDatePollReminderIntegrationTest {
    @Autowired
    private lateinit var meetingDatePollService: MeetingDatePollService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingScheduleResponseRepository: MeetingScheduleResponseRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

    @MockitoBean
    private lateinit var clock: Clock

    @AfterEach
    fun cleanUp() {
        notificationOutboxRepository.deleteAllInBatch()
        notificationRepository.deleteAllInBatch()
        meetingScheduleResponseRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `리마인드를 요청하면 미응답 참여자에게만 알림과 Outbox가 저장된다`() {
        val context = saveReminderContext()
        `when`(clock.instant()).thenReturn(REMINDED_AT)

        meetingDatePollService.remindUnrespondedParticipants(context.meetingId, context.ownerUserId)

        val notifications = notificationRepository.findAll()
        assertEquals(listOf(context.unrespondedUserId), notifications.map { notification -> notification.recipient.id })
        assertEquals(NotificationType.DATE_POLL_REMINDER, notifications.single().type)
        assertEquals(1L, notificationOutboxRepository.count())
    }

    @Test
    fun `같은 시각에 리마인드를 다시 요청해도 새 알림이 저장된다`() {
        val context = saveReminderContext()
        `when`(clock.instant()).thenReturn(REMINDED_AT)

        repeat(2) {
            meetingDatePollService.remindUnrespondedParticipants(context.meetingId, context.ownerUserId)
        }

        assertEquals(2L, notificationRepository.count())
        assertEquals(2L, notificationOutboxRepository.count())
    }

    private fun saveReminderContext(): ReminderContext {
        val group = groupRepository.save(createGroup())
        val owner = saveGroupMember(group, "owner-subject", "방장")
        val unrespondedMember = saveGroupMember(group, "unresponded-subject", "미응답자")
        val respondedMember = saveGroupMember(group, "responded-subject", "응답자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, owner))
        val participants =
            meetingParticipantRepository.saveAllAndFlush(
                listOf(owner, unrespondedMember, respondedMember).map { member ->
                    MeetingParticipant.create(meeting, member)
                },
            )
        meetingScheduleResponseRepository.saveAndFlush(
            MeetingScheduleResponse.create(
                meeting = meeting,
                participant = participants[2],
                respondedAt = Instant.parse("2026-09-20T00:00:00Z"),
            ),
        )

        return ReminderContext(
            meetingId = requireNotNull(meeting.id),
            ownerUserId = requireNotNull(owner.user.id),
            unrespondedUserId = requireNotNull(unrespondedMember.user.id),
        )
    }

    private fun createGroup(): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
        )

    private fun createPollMeeting(
        group: Group,
        creator: GroupMember,
    ): Meeting =
        Meeting.createPoll(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
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

    private data class ReminderContext(
        val meetingId: Long,
        val ownerUserId: Long,
        val unrespondedUserId: Long,
    )

    companion object {
        private val REMINDED_AT: Instant = Instant.parse("2026-09-21T00:00:00Z")

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
