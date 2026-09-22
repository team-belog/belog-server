package org.com.belog.prelog.service

import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingRepository
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PreLogServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val clock = Clock.fixed(Instant.parse("2026-09-22T00:00:00Z"), ZoneOffset.UTC)
    private val preLogService = PreLogService(meetingRepository, groupMemberRepository, clock)

    @Test
    fun `만남 참여자가 아니어도 해당 그룹의 멤버라면 Pre-log 메인 정보를 조회할 수 있다`() {
        stubPreLogMain(canEditMeeting = false)

        val result = preLogService.getPreLogMain(meetingId = 7L, userId = 15L)

        assertEquals(7L, result.meetingId)
        assertEquals("광주 여행", result.meetingName)
        assertEquals(3L, result.groupId)
        assertEquals("여행 모임", result.groupName)
        assertEquals(MeetingStatus.CONFIRMED, result.meetingStatus)
        assertEquals(LocalDate.of(2026, 9, 23), result.startDate)
        assertEquals(LocalDate.of(2026, 9, 24), result.endDate)
        assertEquals("광주광역시", result.location)
        assertFalse(result.isEnded)
        assertFalse(result.canEditMeeting)
    }

    @Test
    fun `만남 생성자는 만남 정보를 수정할 수 있다`() {
        stubPreLogMain(canEditMeeting = true)

        val result = preLogService.getPreLogMain(meetingId = 7L, userId = 15L)

        assertTrue(result.canEditMeeting)
    }

    @Test
    fun `만남 생성자가 아닌 그룹 멤버는 만남 정보를 수정할 수 없다`() {
        stubPreLogMain(canEditMeeting = false)

        val result = preLogService.getPreLogMain(meetingId = 7L, userId = 15L)

        assertFalse(result.canEditMeeting)
    }

    private fun stubPreLogMain(canEditMeeting: Boolean) {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        val groupMember = mock(GroupMember::class.java)
        val currentDate = LocalDate.of(2026, 9, 22)

        `when`(group.id).thenReturn(3L)
        `when`(group.name).thenReturn("여행 모임")
        `when`(meeting.id).thenReturn(7L)
        `when`(meeting.name).thenReturn("광주 여행")
        `when`(meeting.group).thenReturn(group)
        `when`(meeting.status).thenReturn(MeetingStatus.CONFIRMED)
        `when`(meeting.startDate).thenReturn(LocalDate.of(2026, 9, 23))
        `when`(meeting.endDate).thenReturn(LocalDate.of(2026, 9, 24))
        `when`(meeting.location).thenReturn("광주광역시")
        `when`(meeting.isEnded(currentDate)).thenReturn(false)
        `when`(meeting.isCreatedBy(groupMember)).thenReturn(canEditMeeting)
        `when`(meetingRepository.findByIdWithGroupAndCreator(7L)).thenReturn(meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(3L, 15L)).thenReturn(groupMember)
    }
}
