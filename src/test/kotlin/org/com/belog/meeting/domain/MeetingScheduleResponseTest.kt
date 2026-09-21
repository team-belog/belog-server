package org.com.belog.meeting.domain

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.domain.InviteCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.SocialProvider
import org.com.belog.user.domain.User
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class MeetingScheduleResponseTest {
    @Test
    fun `만남 참여자는 후보 일정 응답을 완료할 수 있다`() {
        val context = createPollContext()
        val respondedAt = Instant.parse("2026-09-22T00:00:00Z")

        val response =
            MeetingScheduleResponse.create(
                meeting = context.meeting,
                participant = context.participant,
                respondedAt = respondedAt,
            )

        assertSame(context.meeting, response.meeting)
        assertSame(context.participant, response.participant)
        assertEquals(respondedAt, response.respondedAt)
    }

    @Test
    fun `만남 생성자는 후보 일정에 응답할 수 없다`() {
        val group = createGroup("AB12CD")
        val creator = GroupMember.createOwner(group, completedUser("creator-subject", "생성자"))
        val meeting = createPollMeeting(group, creator)
        val creatorParticipant = MeetingParticipant.create(meeting, creator)

        assertFailsWith<IllegalArgumentException> {
            MeetingScheduleResponse.create(
                meeting = meeting,
                participant = creatorParticipant,
                respondedAt = Instant.parse("2026-09-22T00:00:00Z"),
            )
        }
    }

    @Test
    fun `다른 만남의 참여자는 후보 일정에 응답할 수 없다`() {
        val context = createPollContext()
        val otherMeeting = createPollMeeting(context.group, context.creator)

        assertFailsWith<IllegalArgumentException> {
            MeetingScheduleResponse.create(
                meeting = otherMeeting,
                participant = context.participant,
                respondedAt = Instant.parse("2026-09-22T00:00:00Z"),
            )
        }
    }

    @Test
    fun `확정 날짜 방식의 만남에는 후보 일정 응답을 등록할 수 없다`() {
        val group = createGroup("AB12CD")
        val creator = GroupMember.createOwner(group, completedUser("creator-subject", "생성자"))
        val member = GroupMember.createMember(group, completedUser("member-subject", "참여자"))
        val meeting = createFixedMeeting(group, creator)
        val participant = MeetingParticipant.create(meeting, member)

        assertFailsWith<IllegalArgumentException> {
            MeetingScheduleResponse.create(
                meeting = meeting,
                participant = participant,
                respondedAt = Instant.parse("2026-09-22T00:00:00Z"),
            )
        }
    }

    @Test
    fun `응답에 같은 만남의 가능한 후보 일정을 연결한다`() {
        val context = createPollContext()
        val response = createResponse(context)
        val candidateDateRange = createCandidateDateRange(context.meeting)

        val availableDate = MeetingAvailableDate.create(response, candidateDateRange)

        assertSame(response, availableDate.response)
        assertSame(candidateDateRange, availableDate.candidateDateRange)
    }

    @Test
    fun `다른 만남의 후보 일정은 응답에 연결할 수 없다`() {
        val context = createPollContext()
        val response = createResponse(context)
        val otherMeeting = createPollMeeting(context.group, context.creator)
        val otherCandidateDateRange = createCandidateDateRange(otherMeeting)

        assertFailsWith<IllegalArgumentException> {
            MeetingAvailableDate.create(response, otherCandidateDateRange)
        }
    }

    private fun createPollContext(): PollContext {
        val group = createGroup("AB12CD")
        val creator = GroupMember.createOwner(group, completedUser("creator-subject", "생성자"))
        val member = GroupMember.createMember(group, completedUser("member-subject", "참여자"))
        val meeting = createPollMeeting(group, creator)
        val participant = MeetingParticipant.create(meeting, member)
        return PollContext(group, creator, meeting, participant)
    }

    private fun createResponse(context: PollContext): MeetingScheduleResponse =
        MeetingScheduleResponse.create(
            meeting = context.meeting,
            participant = context.participant,
            respondedAt = Instant.parse("2026-09-22T00:00:00Z"),
        )

    private fun createCandidateDateRange(meeting: Meeting): MeetingCandidateDateRange =
        MeetingCandidateDateRange.create(
            meeting = meeting,
            startDate = LocalDate.of(2026, 9, 23),
            endDate = LocalDate.of(2026, 9, 24),
            currentDate = LocalDate.of(2026, 9, 22),
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

    private fun createFixedMeeting(
        group: Group,
        creator: GroupMember,
    ): Meeting =
        Meeting.createFixed(
            group = group,
            creator = creator,
            name = "광주 여행",
            location = null,
            startDate = LocalDate.of(2026, 9, 23),
            endDate = LocalDate.of(2026, 9, 24),
            confirmedAt = Instant.parse("2026-09-22T00:00:00Z"),
            currentDate = LocalDate.of(2026, 9, 22),
        )

    private fun createGroup(inviteCode: String): Group =
        Group.create(
            name = "주말 여행 모임",
            coverImageObjectKey = null,
            inviteCode = InviteCode.create(inviteCode),
        )

    private fun completedUser(
        providerUserId: String,
        nickname: String,
    ): User =
        User
            .createSocialUser(
                email = "$providerUserId@example.com",
                provider = SocialProvider.GOOGLE,
                providerUserId = providerUserId,
            ).apply {
                completeOnboarding(
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
            }

    private data class PollContext(
        val group: Group,
        val creator: GroupMember,
        val meeting: Meeting,
        val participant: MeetingParticipant,
    )
}
