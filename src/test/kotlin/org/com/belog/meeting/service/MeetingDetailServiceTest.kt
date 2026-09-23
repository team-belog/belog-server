package org.com.belog.meeting.service

import org.com.belog.billlog.repository.BillRepository
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.Group
import org.com.belog.group.domain.GroupMember
import org.com.belog.group.repository.GroupMemberRepository
import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingLogStatus
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.repository.MeetingRepository
import org.com.belog.prelog.repository.PlanRepository
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MeetingDetailServiceTest {
    private val meetingRepository = mock(MeetingRepository::class.java)
    private val groupMemberRepository = mock(GroupMemberRepository::class.java)
    private val planRepository = mock(PlanRepository::class.java)
    private val billRepository = mock(BillRepository::class.java)
    private val meetingDetailService =
        MeetingDetailService(
            meetingRepository = meetingRepository,
            groupMemberRepository = groupMemberRepository,
            planRepository = planRepository,
            billRepository = billRepository,
        )

    @ParameterizedTest
    @CsvSource(
        "false, false, NOT_STARTED, NOT_STARTED",
        "true, false, IN_PROGRESS, NOT_STARTED",
        "false, true, NOT_STARTED, IN_PROGRESS",
        "true, true, IN_PROGRESS, IN_PROGRESS",
    )
    fun `그룹 멤버가 조회하면 데이터 존재 여부에 따라 로그 상태를 반환한다`(
        hasPreLog: Boolean,
        hasBillLog: Boolean,
        expectedPreLogStatus: MeetingLogStatus,
        expectedBillLogStatus: MeetingLogStatus,
    ) {
        stubMeetingDetail()
        `when`(planRepository.existsByMeetingId(MEETING_ID)).thenReturn(hasPreLog)
        `when`(billRepository.existsByMeetingId(MEETING_ID)).thenReturn(hasBillLog)

        val result = meetingDetailService.getMeetingDetail(meetingId = MEETING_ID, userId = USER_ID)

        assertEquals(expectedPreLogStatus, result.preLogStatus)
        assertEquals(expectedBillLogStatus, result.billLogStatus)
    }

    @Test
    fun `그룹 멤버가 아니면 만남 상세 정보를 조회할 수 없다`() {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        `when`(group.id).thenReturn(GROUP_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(meetingRepository.findByIdWithGroupAndCreator(MEETING_ID)).thenReturn(meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(null)

        val exception =
            assertFailsWith<BusinessException> {
                meetingDetailService.getMeetingDetail(meetingId = MEETING_ID, userId = USER_ID)
            }

        assertEquals(GroupErrorCode.NOT_GROUP_MEMBER, exception.errorCode)
        verifyNoInteractions(planRepository, billRepository)
    }

    private fun stubMeetingDetail() {
        val group = mock(Group::class.java)
        val meeting = mock(Meeting::class.java)
        val groupMember = mock(GroupMember::class.java)

        `when`(group.id).thenReturn(GROUP_ID)
        `when`(group.name).thenReturn("피놀리와 기니휘기")
        `when`(meeting.id).thenReturn(MEETING_ID)
        `when`(meeting.group).thenReturn(group)
        `when`(meeting.name).thenReturn("1박 2일 광주 여행")
        `when`(meeting.scheduleType).thenReturn(MeetingScheduleType.FIXED)
        `when`(meeting.status).thenReturn(MeetingStatus.CONFIRMED)
        `when`(meeting.startDate).thenReturn(LocalDate.of(2026, 10, 3))
        `when`(meeting.endDate).thenReturn(LocalDate.of(2026, 10, 4))
        `when`(meeting.location).thenReturn("광주광역시 000 000")
        `when`(meeting.isCreatedBy(groupMember)).thenReturn(false)
        `when`(meetingRepository.findByIdWithGroupAndCreator(MEETING_ID)).thenReturn(meeting)
        `when`(groupMemberRepository.findByGroupIdAndUserId(GROUP_ID, USER_ID)).thenReturn(groupMember)
    }

    companion object {
        private const val MEETING_ID = 7L
        private const val GROUP_ID = 1L
        private const val USER_ID = 15L
    }
}
