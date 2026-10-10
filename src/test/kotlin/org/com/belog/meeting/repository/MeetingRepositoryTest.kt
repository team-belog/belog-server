package org.com.belog.meeting.repository

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
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.user.config.AccountNumberEncryptionConfig
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.com.belog.user.infrastructure.AccountNumberAttributeConverter
import org.com.belog.user.repository.UserRepository
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.data.domain.PageRequest
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@DataJpaTest
@ActiveProfiles("test")
@Import(
    JpaAuditingConfig::class,
    AccountNumberEncryptionConfig::class,
    AccountNumberAttributeConverter::class,
)
class MeetingRepositoryTest {
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

    @Test
    fun `동일한 멤버를 같은 만남에 중복 저장할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meeting = meetingRepository.saveAndFlush(createMeeting(group, creator))
        meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))

        assertFailsWith<DataIntegrityViolationException> {
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, creator))
        }
    }

    @Test
    fun `동일한 그룹 멤버는 서로 다른 만남에 참여할 수 있다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val firstMeeting = meetingRepository.save(createMeeting(group, creator, "첫 번째 만남"))
        val secondMeeting = meetingRepository.save(createMeeting(group, creator, "두 번째 만남"))
        meetingRepository.flush()

        val participants =
            meetingParticipantRepository.saveAllAndFlush(
                listOf(
                    MeetingParticipant.create(firstMeeting, creator),
                    MeetingParticipant.create(secondMeeting, creator),
                ),
            )

        assertEquals(2, participants.size)
        assertEquals(1L, meetingParticipantRepository.countByMeetingId(requireNotNull(firstMeeting.id)))
        assertEquals(1L, meetingParticipantRepository.countByMeetingId(requireNotNull(secondMeeting.id)))
    }

    @Test
    fun `같은 만남에 동일한 후보 일정 범위를 중복 저장할 수 없다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        val startDate = LocalDate.of(2026, 9, 21)
        val endDate = LocalDate.of(2026, 9, 22)
        meetingCandidateDateRangeRepository.saveAndFlush(
            createCandidateDateRange(meeting, startDate, endDate),
        )

        assertFailsWith<DataIntegrityViolationException> {
            meetingCandidateDateRangeRepository.saveAndFlush(
                createCandidateDateRange(meeting, startDate, endDate),
            )
        }
    }

    @Test
    fun `같은 응답에 동일한 후보 일정을 중복 저장할 수 없다`() {
        val context = savePollResponseContext()
        val response = saveScheduleResponse(context)
        val candidate =
            meetingCandidateDateRangeRepository.saveAndFlush(
                createCandidateDateRange(
                    context.meeting,
                    LocalDate.of(2026, 9, 21),
                    LocalDate.of(2026, 9, 22),
                ),
            )
        meetingAvailableDateRepository.saveAndFlush(MeetingAvailableDate.create(response, candidate))

        assertFailsWith<DataIntegrityViolationException> {
            meetingAvailableDateRepository.saveAndFlush(MeetingAvailableDate.create(response, candidate))
        }
    }

    @Test
    fun `같은 참여자는 하나의 만남에 한 번만 응답할 수 있다`() {
        val context = savePollResponseContext()
        saveScheduleResponse(context)

        assertFailsWith<DataIntegrityViolationException> {
            meetingScheduleResponseRepository.saveAndFlush(
                MeetingScheduleResponse.create(
                    meeting = context.meeting,
                    participant = context.participant,
                    respondedAt = Instant.parse("2026-09-20T02:00:00Z"),
                ),
            )
        }
    }

    @Test
    fun `미응답 참여자 조회는 생성자와 응답한 참여자와 탈퇴한 멤버를 제외한다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val unrespondedMember = saveGroupMember(group, "unresponded-subject", "미응답자")
        val respondedMember = saveGroupMember(group, "responded-subject", "응답자")
        val withdrawnMember = saveGroupMember(group, "withdrawn-subject", "탈퇴자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        val participants =
            meetingParticipantRepository.saveAllAndFlush(
                listOf(creator, unrespondedMember, respondedMember, withdrawnMember).map { member ->
                    MeetingParticipant.create(meeting, member)
                },
            )
        saveScheduleResponse(PollResponseContext(meeting, participants[2]))
        withdrawnMember.withdraw(Instant.parse("2026-09-20T02:00:00Z"))
        groupMemberRepository.flush()

        val result = meetingParticipantRepository.findAllUnrespondedActiveWithUserByMeetingId(requireNotNull(meeting.id))

        assertEquals(listOf(unrespondedMember.id), result.map { participant -> participant.groupMember.id })
    }

    @Test
    fun `삭제된 만남은 모든 활성 만남 조회에서 제외된다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val confirmedMeeting = meetingRepository.save(createMeeting(group, creator))
        val schedulingMeeting = meetingRepository.save(createPollMeeting(group, creator))
        meetingRepository.flush()

        confirmedMeeting.delete(Instant.parse("2026-09-23T00:00:00Z"))
        schedulingMeeting.delete(Instant.parse("2026-09-23T00:00:00Z"))
        meetingRepository.flush()

        val confirmedMeetingId = requireNotNull(confirmedMeeting.id)
        val groupId = requireNotNull(group.id)

        assertNull(meetingRepository.findActiveById(confirmedMeetingId))
        assertFalse(meetingRepository.existsByIdAndDeletedAtIsNull(confirmedMeetingId))
        assertNull(meetingRepository.findByIdWithGroup(confirmedMeetingId))
        assertNull(meetingRepository.findByIdWithGroupForUpdate(confirmedMeetingId))
        assertNull(meetingRepository.findByIdWithGroupOwnerAndCreator(confirmedMeetingId))
        assertNull(meetingRepository.findByIdForUpdate(confirmedMeetingId))
        assertTrue(meetingRepository.findSchedulingMeetings(groupId, MeetingStatus.SCHEDULING).isEmpty())
        assertTrue(
            meetingRepository
                .findActiveMeetings(groupId, LocalDate.of(2026, 9, 21), MeetingStatus.CONFIRMED)
                .isEmpty(),
        )
        assertTrue(
            meetingRepository
                .findPastMeetingPage(
                    groupId = groupId,
                    currentDate = LocalDate.of(2026, 9, 23),
                    cursor = null,
                    status = MeetingStatus.CONFIRMED,
                    pageable = PageRequest.of(0, 10),
                ).isEmpty(),
        )
    }

    @Test
    fun `시작일로 만남 ID를 조회하면 삭제된 만남과 시작일이 다른 만남은 제외된다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val target = meetingRepository.save(createMeetingStartingOn(group, creator, REMINDER_START_DATE))
        val deleted = createMeetingStartingOn(group, creator, REMINDER_START_DATE)
        deleted.delete(Instant.parse("2026-09-20T00:00:00Z"))
        meetingRepository.save(deleted)
        meetingRepository.save(createMeetingStartingOn(group, creator, REMINDER_START_DATE.plusDays(1)))
        meetingRepository.saveAndFlush(createPollMeeting(group, creator))

        val meetingIds =
            meetingRepository.findIdsByStartDate(
                startDate = REMINDER_START_DATE,
                cursor = null,
                status = MeetingStatus.CONFIRMED,
                pageable = PageRequest.of(0, 10),
            )

        assertEquals(listOf(target.id), meetingIds)
    }

    @Test
    fun `시작일로 만남 ID를 조회하면 커서 이후 ID를 요청 개수만큼 오름차순으로 반환한다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val meetings =
            meetingRepository.saveAllAndFlush(
                List(4) { createMeetingStartingOn(group, creator, REMINDER_START_DATE) },
            )

        val meetingIds =
            meetingRepository.findIdsByStartDate(
                startDate = REMINDER_START_DATE,
                cursor = meetings[0].id,
                status = MeetingStatus.CONFIRMED,
                pageable = PageRequest.of(0, 2),
            )

        assertEquals(listOf(meetings[1].id, meetings[2].id), meetingIds)
    }

    @Test
    fun `종료일로 만남 ID를 조회하면 삭제된 만남과 종료일이 다른 만남은 제외된다`() {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val endDate = REMINDER_START_DATE.plusDays(1)
        val target = meetingRepository.save(createMeetingStartingOn(group, creator, REMINDER_START_DATE))
        val deleted = createMeetingStartingOn(group, creator, REMINDER_START_DATE)
        deleted.delete(Instant.parse("2026-09-20T00:00:00Z"))
        meetingRepository.save(deleted)
        meetingRepository.save(createMeetingStartingOn(group, creator, endDate))
        meetingRepository.saveAndFlush(createPollMeeting(group, creator))

        val meetingIds =
            meetingRepository.findIdsByEndDate(
                endDate = endDate,
                cursor = null,
                status = MeetingStatus.CONFIRMED,
                pageable = PageRequest.of(0, 10),
            )

        assertEquals(listOf(target.id), meetingIds)
    }

    private fun createMeetingStartingOn(
        group: Group,
        creator: GroupMember,
        startDate: LocalDate,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            dateRange = MeetingDateRange(startDate, startDate.plusDays(1)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun createMeeting(
        group: Group,
        creator: GroupMember,
        name: String = "광주 여행",
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = name,
            location = "서울고속버스터미널",
            dateRange = MeetingDateRange(LocalDate.of(2026, 9, 21), LocalDate.of(2026, 9, 22)),
            confirmedAt = Instant.parse("2026-09-20T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun createPollMeeting(
        group: Group,
        creator: GroupMember,
    ): Meeting =
        Meeting.createPoll(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = "서울고속버스터미널",
        )

    private fun createCandidateDateRange(
        meeting: Meeting,
        startDate: LocalDate,
        endDate: LocalDate,
    ): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            dateRange = MeetingDateRange(startDate, endDate),
            currentDate = LocalDate.of(2026, 9, 20),
        )

    private fun savePollResponseContext(): PollResponseContext {
        val group = groupRepository.save(createGroup("AB12CD"))
        val creator = saveGroupMember(group, "creator-subject", "생성자")
        val member = saveGroupMember(group, "member-subject", "참여자")
        val meeting = meetingRepository.saveAndFlush(createPollMeeting(group, creator))
        val participant =
            meetingParticipantRepository.saveAndFlush(MeetingParticipant.create(meeting, member))
        return PollResponseContext(meeting, participant)
    }

    private fun saveScheduleResponse(context: PollResponseContext): MeetingScheduleResponse =
        meetingScheduleResponseRepository.saveAndFlush(
            MeetingScheduleResponse.create(
                meeting = context.meeting,
                participant = context.participant,
                respondedAt = Instant.parse("2026-09-20T01:00:00Z"),
            ),
        )

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun saveGroupMember(
        group: Group,
        providerUserId: String,
        nickname: String,
    ): GroupMember {
        val user = saveCompletedUser(providerUserId, nickname)
        return groupMemberRepository.saveAndFlush(GroupMember.createMember(group, user))
    }

    private fun saveCompletedUser(
        providerUserId: String,
        nickname: String,
    ): User {
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
        return userRepository.saveAndFlush(user)
    }

    private data class PollResponseContext(
        val meeting: Meeting,
        val participant: MeetingParticipant,
    )

    companion object {
        private val REMINDER_START_DATE: LocalDate = LocalDate.of(2026, 9, 28)
    }
}
