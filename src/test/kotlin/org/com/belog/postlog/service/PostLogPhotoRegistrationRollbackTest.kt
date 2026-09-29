package org.com.belog.postlog.service

import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.domain.PostLogPhoto
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
import org.mockito.ArgumentMatchers.anyList
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

@DataJpaTest
@ActiveProfiles("test")
@Import(
    PostLogPhotoRegistrationService::class,
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class PostLogPhotoRegistrationRollbackTest {
    @Autowired
    private lateinit var registrationService: PostLogPhotoRegistrationService

    @Autowired
    private lateinit var postLogRepository: PostLogRepository

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @MockitoBean
    private lateinit var photoRepository: PostLogPhotoRepository

    @AfterEach
    fun cleanUp() {
        postLogRepository.deleteAllInBatch()
        meetingRepository.deleteAllInBatch()
        groupMemberRepository.deleteAllInBatch()
        groupRepository.deleteAllInBatch()
        userRepository.deleteAllInBatch()
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `사진 일괄 저장 중 실패하면 새로 생성한 Post-log도 롤백된다`() {
        val group = groupRepository.save(createGroup())
        val member = saveGroupMember(group)
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, member))
        val meetingId = requireNotNull(meeting.id)
        val target =
            ValidatedPostLogPhotoRegistrationTarget(
                objectKey = PostLogPhotoObjectKey.create(meetingId, "post-logs/$meetingId/photos/photo.jpg"),
                capturedAt = OffsetDateTime.parse("2026-09-28T14:37:21+09:00"),
            )
        `when`(photoRepository.findAllByObjectKeyIn(listOf(target.objectKey.value))).thenReturn(emptyList())
        doThrow(IllegalStateException("사진 저장 실패"))
            .`when`(photoRepository)
            .saveAll(anyList<PostLogPhoto>())

        assertFailsWith<IllegalStateException> {
            registrationService.register(
                meetingId = meetingId,
                userId = requireNotNull(member.user.id),
                targets = listOf(target),
            )
        }

        assertEquals(0L, postLogRepository.count())
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
}
