package org.com.belog.postlog.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.com.belog.postlog.repository.PostLogPhotoRepository
import org.com.belog.postlog.repository.PostLogRepository
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PostLogPhotoRegistrationConcurrencyTest {
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
        photoRepository.deleteAll()
        postLogRepository.deleteAll()
        meetingRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `동일한 모임에 첫 사진을 동시에 등록해도 Post-log는 하나만 생성된다`() {
        val group = groupRepository.save(createGroup())
        val member = saveGroupMember(group)
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, member))
        val meetingId = requireNotNull(meeting.id)
        val userId = requireNotNull(member.user.id)
        val targets =
            listOf(
                target(meetingId, "first.jpg", "2026-09-28T14:37:21+09:00"),
                target(meetingId, "second.webp", "2026-09-28T14:38:21+09:00"),
            )
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val requests =
                targets.map { target ->
                    executor.submit<Long> {
                        startSignal.await()
                        requireNotNull(registrationService.register(meetingId, userId, listOf(target)).single().id)
                    }
                }

            startSignal.countDown()
            val photoIds = requests.map { request -> request.get(10, TimeUnit.SECONDS) }

            assertEquals(2, photoIds.distinct().size)
            assertEquals(1L, postLogRepository.count())
            assertEquals(2L, photoRepository.count())
        } finally {
            executor.shutdownNow()
        }
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

    companion object {
        private const val CONCURRENT_REQUEST_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
