package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingAvailableDateRepository
import org.com.belog.meeting.repository.MeetingCandidateDateRangeRepository
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.repository.MeetingScheduleResponseRepository
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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.assertEquals

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class MeetingDatePollServiceConcurrencyTest {
    @Autowired
    private lateinit var meetingDatePollService: MeetingDatePollService

    @Autowired
    private lateinit var meetingRepository: MeetingRepository

    @Autowired
    private lateinit var meetingParticipantRepository: MeetingParticipantRepository

    @Autowired
    private lateinit var meetingCandidateDateRangeRepository: MeetingCandidateDateRangeRepository

    @Autowired
    private lateinit var meetingScheduleResponseRepository: MeetingScheduleResponseRepository

    @Autowired
    private lateinit var meetingAvailableDateRepository: MeetingAvailableDateRepository

    @Autowired
    private lateinit var groupRepository: GroupRepository

    @Autowired
    private lateinit var groupMemberRepository: GroupMemberRepository

    @Autowired
    private lateinit var userRepository: UserRepository

    @AfterEach
    fun cleanUp() {
        meetingAvailableDateRepository.deleteAll()
        meetingScheduleResponseRepository.deleteAll()
        meetingCandidateDateRangeRepository.deleteAll()
        meetingParticipantRepository.deleteAll()
        meetingRepository.deleteAll()
        groupMemberRepository.deleteAll()
        groupRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `동일한 참여자가 동시에 응답해도 한 번만 저장된다`() {
        val group = groupRepository.save(createGroup())
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        meetingParticipantRepository.saveAllAndFlush(
            listOf(
                MeetingParticipant.create(meeting, creator),
                MeetingParticipant.create(meeting, member),
            ),
        )
        val meetingId = requireNotNull(meeting.id)
        val userId = requireNotNull(member.user.id)
        val executor = Executors.newFixedThreadPool(CONCURRENT_RESPONSE_COUNT)
        val startSignal = CountDownLatch(1)

        try {
            val responses =
                (1..CONCURRENT_RESPONSE_COUNT).map {
                    executor.submit<MeetingErrorCode?> {
                        startSignal.await()
                        try {
                            meetingDatePollService.respondDatePoll(meetingId, userId, emptyList())
                            null
                        } catch (exception: BusinessException) {
                            exception.errorCode as MeetingErrorCode
                        }
                    }
                }

            startSignal.countDown()
            val results = responses.map { response -> response.get(10, TimeUnit.SECONDS) }

            assertEquals(1, results.count { it == null })
            assertEquals(1, results.count { it == MeetingErrorCode.DATE_POLL_ALREADY_RESPONDED })
            assertEquals(1L, meetingScheduleResponseRepository.count())
            assertEquals(0L, meetingAvailableDateRepository.count())
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
        private const val CONCURRENT_RESPONSE_COUNT = 2

        @Container
        @ServiceConnection
        @JvmField
        val mysql = MySQLContainer("mysql:8.4")
    }
}
