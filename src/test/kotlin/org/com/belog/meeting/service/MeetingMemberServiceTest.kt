package org.com.belog.meeting.service

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.meeting.repository.MeetingParticipantRepository
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.meeting.service.result.MeetingMemberResult
import org.com.belog.user.domain.User
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeetingMemberServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val meetingParticipantRepository = mock(MeetingParticipantRepository::class.java)
    private val userService = mock(UserService::class.java)
    private val meetingMemberService =
        MeetingMemberService(meetingRepository, groupMemberRepository, meetingParticipantRepository, userService)

    @Test
    fun `요청자를 먼저 반환하고 나머지는 그룹 멤버 ID 순으로 정렬한다`() {
        stubAccessibleMeeting()
        val first = stubParticipant(30L, 3L, "첫멤버", null)
        val requester = stubParticipant(50L, USER_ID, "요청자", "https://example.com/profile")
        val last = stubParticipant(70L, 7L, "끝멤버", null)
        `when`(meetingParticipantRepository.findAllActiveWithUserByMeetingId(MEETING_ID))
            .thenReturn(listOf(last, requester, first))

        val results = meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)

        assertEquals(
            listOf(
                MeetingMemberResult(50L, "요청자", "https://example.com/profile", true),
                MeetingMemberResult(30L, "첫멤버", null, false),
                MeetingMemberResult(70L, "끝멤버", null, false),
            ),
            results,
        )
    }

    @Test
    fun `만남에 참여하지 않은 그룹 멤버도 조회할 수 있지만 목록에는 추가하지 않는다`() {
        stubAccessibleMeeting()
        val first = stubParticipant(30L, 3L, "첫멤버", null)
        val last = stubParticipant(70L, 7L, "끝멤버", null)
        `when`(meetingParticipantRepository.findAllActiveWithUserByMeetingId(MEETING_ID))
            .thenReturn(listOf(last, first))

        val results = meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)

        assertEquals(
            listOf(
                MeetingMemberResult(30L, "첫멤버", null, false),
                MeetingMemberResult(70L, "끝멤버", null, false),
            ),
            results,
        )
    }

    @Test
    fun `조회 가능한 만남이 없으면 만남 조회 실패를 반환한다`() {
        val exception =
            assertFailsWith<BusinessException> {
                meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)
            }

        assertEquals(MeetingErrorCode.MEETING_NOT_FOUND, exception.errorCode)
    }

    @Test
    fun `활성 그룹 멤버가 아니면 만남 멤버를 조회할 수 없다`() {
        stubAccessibleMeeting()
        `when`(groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID))
            .thenReturn(false)

        val exception =
            assertFailsWith<BusinessException> {
                meetingMemberService.getMeetingMembers(MEETING_ID, USER_ID)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
    }

    private fun stubAccessibleMeeting() {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(meetingRepository.findByIdWithGroup(MEETING_ID)).thenReturn(meeting)
        `when`(groupMemberRepository.existsByGroupIdAndUserIdAndWithdrawnAtIsNull(GROUP_ID, USER_ID))
            .thenReturn(true)
    }

    private fun stubParticipant(
        memberId: Long,
        userId: Long,
        nickname: String,
        profileImageUrl: String?,
    ): MeetingParticipant {
        val user = mock(User::class.java)
        val member = mock(GroupMember::class.java)
        val participant = mock(MeetingParticipant::class.java)
        `when`(user.id).thenReturn(userId)
        `when`(member.id).thenReturn(memberId)
        `when`(member.user).thenReturn(user)
        `when`(participant.groupMember).thenReturn(member)
        `when`(userService.resolveDisplayNickname(user)).thenReturn(nickname)
        `when`(userService.resolveProfileImageUrl(user)).thenReturn(profileImageUrl)
        return participant
    }

    companion object {
        private const val GROUP_ID = 1L
        private const val MEETING_ID = 2L
        private const val USER_ID = 15L
    }
}
