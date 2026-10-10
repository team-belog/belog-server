package org.com.belog.postlog.service

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
import org.com.belog.notification.repository.NotificationOutboxRepository
import org.com.belog.notification.repository.NotificationRepository
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.repository.PostLogPhotoRepository
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
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.ActiveProfiles
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.mysql.MySQLContainer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostLogPhotoRegistrationNotificationIntegrationTest {
    @Autowired
    private lateinit var registrationService: PostLogPhotoRegistrationService

    @Autowired
    private lateinit var photoRepository: PostLogPhotoRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @Autowired
    private lateinit var notificationRepository: NotificationRepository

    @Autowired
    private lateinit var notificationOutboxRepository: NotificationOutboxRepository

    @AfterEach
    fun cleanUp() {
        notificationOutboxRepository.deleteAllInBatch()
        notificationRepository.deleteAllInBatch()
        photoRepository.deleteAllInBatch()
        meetingParticipantRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `사진을 여러 장 등록하면 업로더를 제외한 만남 참여자에게 사진 등록 알림을 한 건씩 저장한다`() {
        val context = saveMeetingContext()
        val targets =
            listOf(
                target(context.meetingId, "first.jpg", "2026-09-28T14:37:21+09:00"),
                target(context.meetingId, "second.webp", "2026-09-28T12:08:31+09:00"),
            )

        val photos = registrationService.register(context.meetingId, context.uploaderUserId, targets)

        val firstPhotoId = photos.minOf { photo -> requireNotNull(photo.id) }
        val notifications = notificationRepository.findAll().sortedBy { notification -> notification.recipient.id }
        assertEquals(context.participantUserIds, notifications.map { notification -> notification.recipient.id })
        notifications.forEach { notification ->
            assertEquals(NotificationType.POST_LOG_PHOTOS_REGISTERED, notification.type)
            assertEquals(context.uploaderUserId, notification.actor?.id)
            assertEquals("업로더 님이 사진을 등록했어요", notification.message)
            assertEquals(context.meetingId, notification.targetId)
            assertEquals(
                "POST_LOG_PHOTOS_REGISTERED:$firstPhotoId:${notification.recipient.id}",
                notification.deduplicationKey,
            )
        }
        assertEquals(context.participantUserIds.size.toLong(), notificationOutboxRepository.count())
    }

    @Test
    fun `이미 등록된 사진만 다시 등록하면 사진 등록 알림을 저장하지 않는다`() {
        val context = saveMeetingContext()
        val existingTarget = target(context.meetingId, "existing.jpg", "2026-09-28T14:37:21+09:00")
        registrationService.register(context.meetingId, context.uploaderUserId, listOf(existingTarget))

        registrationService.register(context.meetingId, context.uploaderUserId, listOf(existingTarget))

        assertEquals(context.participantUserIds.size.toLong(), notificationRepository.count())
    }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val uploader = saveGroupMember(group, "uploader", "업로더")
        val participants = listOf(saveGroupMember(group, "first", "참여자1"), saveGroupMember(group, "second", "참여자2"))
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, uploader))
        meetingParticipantRepository.saveAllAndFlush(
            (listOf(uploader) + participants).map { member -> MeetingParticipant.create(meeting, member) },
        )
        return MeetingContext(
            meetingId = requireNotNull(meeting.id),
            uploaderUserId = requireNotNull(uploader.user.id),
            participantUserIds = participants.map { participant -> requireNotNull(participant.user.id) },
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
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 28), LocalDate.of(2026, 9, 28)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun saveGroupMember(
        group: Group,
        key: String,
        nickname: String,
    ): GroupMember {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "$key@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "$key-subject",
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
        userRepository.saveAndFlush(user)
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

    private fun target(
        meetingId: Long,
        fileName: String,
        capturedAt: String,
    ): ValidatedPostLogPhotoRegistrationTarget =
        ValidatedPostLogPhotoRegistrationTarget(
            objectKey = PostLogPhotoObjectKey.create(meetingId, "post-logs/$meetingId/photos/$fileName"),
            capturedAt = OffsetDateTime.parse(capturedAt),
        )

    private data class MeetingContext(
        val meetingId: Long,
        val uploaderUserId: Long,
        val participantUserIds: List<Long>,
    )

    companion object {
        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
