package org.com.belog.postlog.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogRepository
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

@DataJpaTest
@ActiveProfiles("test")
@Import(
    PostLogPhotoRegistrationService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PostLogPhotoRegistrationServiceTest {
    @Autowired
    private lateinit var registrationService: PostLogPhotoRegistrationService

    @Autowired
    private lateinit var postLogRepository: PostLogRepository

    @Autowired
    private lateinit var photoRepository: PostLogPhotoRepository

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
        photoRepository.deleteAllInBatch()
        postLogRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    fun `Post-log가 없으면 생성하고 사진 메타데이터를 요청 순서대로 일괄 저장한다`() {
        val context = saveMeetingContext()
        val targets =
            listOf(
                target(context.meetingId, "first.jpg", "2026-09-28T14:37:21+09:00"),
                target(context.meetingId, "second.webp", "2026-09-28T12:08:31+09:00"),
            )

        val photos = registrationService.register(context.meetingId, context.userId, targets)

        val postLog = postLogRepository.findByMeetingId(context.meetingId)
        assertNotNull(postLog)
        assertEquals(1L, postLogRepository.count())
        assertEquals(2L, photoRepository.count())
        assertEquals(targets.map { it.objectKey.value }, photos.map { it.objectKey })
        assertEquals(targets.map { it.capturedAt }, photos.map { it.capturedAtWithOffset() })
    }

    @Test
    fun `기존 Post-log에는 새 사진만 추가한다`() {
        val context = saveMeetingContext()
        registrationService.register(
            context.meetingId,
            context.userId,
            listOf(target(context.meetingId, "first.jpg", "2026-09-28T14:37:21+09:00")),
        )

        registrationService.register(
            context.meetingId,
            context.userId,
            listOf(target(context.meetingId, "second.png", "2026-09-28T15:10:00+09:00")),
        )

        assertEquals(1L, postLogRepository.count())
        assertEquals(2L, photoRepository.count())
    }

    @Test
    fun `동일 요청을 재시도하면 기존 사진을 반환하고 신규 사진만 저장한다`() {
        val context = saveMeetingContext()
        val existingTarget = target(context.meetingId, "existing.jpg", "2026-09-28T14:37:21+09:00")
        val existingPhoto = registrationService.register(context.meetingId, context.userId, listOf(existingTarget)).single()
        val newTarget = target(context.meetingId, "new.webp", "2026-09-28T15:10:00+09:00")

        val photos = registrationService.register(context.meetingId, context.userId, listOf(existingTarget, newTarget))

        assertEquals(1L, postLogRepository.count())
        assertEquals(2L, photoRepository.count())
        assertEquals(existingPhoto.id, photos.first().id)
        assertEquals(listOf(existingTarget.objectKey.value, newTarget.objectKey.value), photos.map { it.objectKey })
    }

    @Test
    fun `한 사진의 object key가 다른 촬영 시각으로 충돌하면 신규 사진도 저장하지 않는다`() {
        val context = saveMeetingContext()
        val existingTarget = target(context.meetingId, "existing.jpg", "2026-09-28T14:37:21+09:00")
        registrationService.register(context.meetingId, context.userId, listOf(existingTarget))
        val newTarget = target(context.meetingId, "new.png", "2026-09-28T15:10:00+09:00")
        val conflictingTarget = existingTarget.copy(capturedAt = OffsetDateTime.parse("2026-09-28T16:00:00+09:00"))

        val exception =
            assertFailsWith<BusinessException> {
                registrationService.register(
                    context.meetingId,
                    context.userId,
                    listOf(newTarget, conflictingTarget),
                )
            }

        assertEquals(PostLogErrorCode.PHOTO_OBJECT_KEY_CONFLICT, exception.errorCode)
        assertEquals(1L, postLogRepository.count())
        assertEquals(1L, photoRepository.count())
        assertEquals(listOf(existingTarget.objectKey.value), photoRepository.findAll().map { it.objectKey })
    }

    private fun saveMeetingContext(): MeetingContext {
        val group = groupRepository.save(createGroup())
        val member = saveGroupMember(group)
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, member))
        return MeetingContext(
            meetingId = requireNotNull(meeting.id),
            userId = requireNotNull(member.user.id),
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

    private fun saveGroupMember(group: Group): GroupMember {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "member@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "member-subject",
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = "멤버",
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
        val userId: Long,
    )
}
