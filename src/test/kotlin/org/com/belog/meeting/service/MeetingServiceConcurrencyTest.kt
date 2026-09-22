package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@SpringBootTest
@ActiveProfiles("test")
@Import(MeetingServiceTest.FixedClockConfig::class)
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MeetingServiceConcurrencyTest {
    @Autowired
    private lateinit var meetingService: MeetingService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        meetingCandidateDateRangeRepository.deleteAll()
        meetingParticipantRepository.deleteAll()
        meetingRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `서로 다른 후보 일정을 동시에 확정하면 하나만 성공한다`() {
        val group = groupRepository.save(createGroup())
        val creator = saveGroupMember(group)
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        val candidates =
            meetingCandidateDateRangeRepository.saveAllAndFlush(
                listOf(
                    createCandidateDateRange(meeting, LocalDate.of(2026, 10, 1)),
                    createCandidateDateRange(meeting, LocalDate.of(2026, 10, 8)),
                ),
            )
        val meetingId = requireNotNull(meeting.id)
        val userId = requireNotNull(creator.user.id)
        val executor = Executors.newFixedThreadPool(CONCURRENT_REQUEST_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val requests =
                candidates.map { candidate ->
                    executor.submit<MeetingErrorCode?> {
                        startSignal.await()
                        try {
                            meetingService.confirmMeetingDate(
                                meetingId = meetingId,
                                userId = userId,
                                candidateDateRangeId = requireNotNull(candidate.id),
                            )
                            null
                        } catch (exception: BusinessException) {
                            exception.errorCode as MeetingErrorCode
                        }
                    }
                }

            startSignal.countDown()
            val results = requests.map { request -> request.get(10, TimeUnit.SECONDS) }
            val confirmedMeeting = meetingRepository.findById(meetingId).orElseThrow()

            assertEquals(1, results.count { it == null })
            assertEquals(1, results.count { it == MeetingErrorCode.MEETING_DATE_NOT_SCHEDULING })
            assertEquals(MeetingStatus.CONFIRMED, confirmedMeeting.status)
            assertTrue(
                candidates.any { candidate ->
                    candidate.startDate == confirmedMeeting.startDate && candidate.endDate == confirmedMeeting.endDate
                },
            )
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

    private fun createCandidateDateRange(
        meeting: Meeting,
        startDate: LocalDate,
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            dateRange = MeetingDateRange(startDate, startDate.plusDays(1)),
            currentDate = LocalDate.of(2026, 9, 22),
        )

    private fun saveGroupMember(group: Group): GroupMember {
        val user =
            userRepository.save(
                User.createSocialUser(
                    email = "creator@example.com",
                    provider = SocialProvider.GOOGLE,
                    providerUserId = "creator-subject",
                ),
            )
        user.completeOnboarding(
            profileImageObjectKey = null,
            nickname = "생성자",
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

    companion object {
        private const val CONCURRENT_REQUEST_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
