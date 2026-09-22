package org.com.belog.meeting.repository

import jakarta.persistence.EntityManager
import jakarta.persistence.EntityManagerFactory
import org.com.belog.global.config.JpaAuditingConfig
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.group.repository.GroupRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingAvailableDate
import org.com.belog.meeting.domain.MeetingCandidateDateRange
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.domain.MeetingScheduleResponse
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.hibernate.SessionFactory
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class MeetingDatePollQueryRepositoryTest {
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

    @Autowired
    private lateinit var entityManager: EntityManager

    @Autowired
    private lateinit var entityManagerFactory: EntityManagerFactory

    @Test
    fun `조율 현황 데이터를 네 번의 일괄 쿼리로 조회한다`() {
        val context = saveDatePollContext()
        entityManager.clear()
        val statistics = entityManagerFactory.unwrap(SessionFactory::class.java).statistics
        statistics.isStatisticsEnabled = true
        statistics.clear()

        val candidates =
            meetingCandidateDateRangeRepository.findAllByMeetingIdOrderByDate(context.meetingId)
        val participants =
            meetingParticipantRepository.findAllWithMemberAndUserByMeetingId(context.meetingId)
        val responses =
            meetingScheduleResponseRepository.findAllWithParticipantByMeetingId(context.meetingId)
        val availableDates =
            meetingAvailableDateRepository
                .findAllWithResponseParticipantAndCandidateByMeetingId(context.meetingId)

        assertEquals(2, candidates.size)
        assertEquals(listOf("생성자", "참여자1", "참여자2"), participants.map { it.groupMember.user.nickname })
        assertEquals(2, responses.size)
        assertEquals(listOf(context.candidateIds.last()), availableDates.map { requireNotNull(it.candidateDateRange.id) })

        participants.forEach { participant ->
            requireNotNull(participant.meeting.createdBy.id)
            requireNotNull(participant.groupMember.user.nickname)
        }
        responses.forEach { response -> requireNotNull(response.participant.id) }
        availableDates.forEach { availableDate ->
            requireNotNull(availableDate.response.participant.id)
            requireNotNull(availableDate.candidateDateRange.id)
        }

        assertEquals(4L, statistics.prepareStatementCount)
    }

    private fun saveDatePollContext(): DatePollContext {
        val group = groupRepository.save(createGroup())
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val firstMember = saveGroupMember(group, "member-1-subject", "참여자1")
        val secondMember = saveGroupMember(group, "member-2-subject", "참여자2")
        val meeting =
            meetingRepository.saveAndFlush(
                Meeting.createPoll(
                    group = group,
                    creator = creator,
                    name = "광주 여행",
                    location = null,
                ),
            )
        val participants =
            meetingParticipantRepository.saveAllAndFlush(
                listOf(creator, firstMember, secondMember).map { member ->
                    MeetingParticipant.create(meeting, member)
                },
            )
        val candidates =
            meetingCandidateDateRangeRepository.saveAllAndFlush(
                listOf(
                    createCandidate(meeting, LocalDate.of(2026, 10, 8)),
                    createCandidate(meeting, LocalDate.of(2026, 10, 1)),
                ),
            )
        val firstResponse =
            meetingScheduleResponseRepository.save(
                MeetingScheduleResponse.create(
                    meeting = meeting,
                    participant = participants[1],
                    respondedAt = Instant.parse("2026-09-22T01:00:00Z"),
                ),
            )
        meetingScheduleResponseRepository.save(
            MeetingScheduleResponse.create(
                meeting = meeting,
                participant = participants[2],
                respondedAt = Instant.parse("2026-09-22T02:00:00Z"),
            ),
        )
        meetingAvailableDateRepository.saveAndFlush(
            MeetingAvailableDate.create(firstResponse, candidates.last()),
        )

        return DatePollContext(
            meetingId = requireNotNull(meeting.id),
            candidateIds = candidates.map { requireNotNull(it.id) },
        )
    }

    private fun createCandidate(
        meeting: Meeting,
        startDate: LocalDate,
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            dateRange = MeetingDateRange(startDate, startDate.plusDays(1)),
            currentDate = LocalDate.of(2026, 9, 22),
        )

    private fun createGroup(): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create("AB12CD"),
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
            completedAt = Instant.parse("2026-09-20T00:00:00Z"),
        )
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

    private data class DatePollContext(
        val meetingId: Long,
        val candidateIds: List<Long>,
    )
}
