package org.com.belog.meeting.controller

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.MeetingService
import org.com.belog.meeting.service.result.CreatedMeeting
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate

@WebMvcTest(MeetingController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MeetingControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var meetingService: MeetingService

    @Test
    fun `확정 날짜 만남을 생성한다`() {
        `when`(
            meetingService.createFixedMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "광주 1박 2일",
                location = "서울고속버스터미널",
                participantMemberIds = listOf(22L, 23L),
                confirmedDate = LocalDate.of(2026, 10, 3),
            ),
        ).thenReturn(
            CreatedMeeting(
                meetingId = 7L,
                groupId = 1L,
                name = "광주 1박 2일",
                location = "서울고속버스터미널",
                scheduleType = MeetingScheduleType.FIXED,
                status = MeetingStatus.CONFIRMED,
                confirmedDate = LocalDate.of(2026, 10, 3),
                confirmedAt = Instant.parse("2026-09-21T00:00:00Z"),
                participantCount = 3,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "광주 1박 2일",
                          "location": "서울고속버스터미널",
                          "participantMemberIds": [22, 23],
                          "schedule": {
                            "type": "FIXED",
                            "date": "2026-10-03"
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("MEETING-S001"))
            .andExpect(jsonPath("$.message").value("만남이 생성되었습니다."))
            .andExpect(jsonPath("$.data.meetingId").value(7))
            .andExpect(jsonPath("$.data.groupId").value(1))
            .andExpect(jsonPath("$.data.name").value("광주 1박 2일"))
            .andExpect(jsonPath("$.data.location").value("서울고속버스터미널"))
            .andExpect(jsonPath("$.data.scheduleType").value("FIXED"))
            .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
            .andExpect(jsonPath("$.data.confirmedDate").value("2026-10-03"))
            .andExpect(jsonPath("$.data.confirmedAt").value("2026-09-21T00:00:00Z"))
            .andExpect(jsonPath("$.data.participantCount").value(3))
    }

    @Test
    fun `만남명이 공백이면 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":" ","schedule":{"type":"FIXED","date":"2026-10-03"}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `초대 인원이 최대 인원을 초과하면 생성을 거절한다`() {
        val participantIds = (1L..15L).joinToString(",")

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","participantMemberIds":[$participantIds],"schedule":{"type":"FIXED","date":"2026-10-03"}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `일정 조율 방식으로 생성 요청하면 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"POLL","date":"2026-10-03"}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEETING-E006"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `날짜 형식이 올바르지 않으면 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"FIXED","date":"2026.10.03"}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E002"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `그룹 멤버가 아니면 만남을 생성할 수 없다`() {
        `when`(
            meetingService.createFixedMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "만남",
                location = null,
                participantMemberIds = emptyList(),
                confirmedDate = LocalDate.of(2026, 10, 3),
            ),
        ).thenThrow(BusinessException(GroupErrorCode.NOT_GROUP_MEMBER))

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"FIXED","date":"2026-10-03"}}""",
                    ),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GROUP-E013"))
    }

    @Test
    fun `다른 그룹의 멤버를 참여자로 지정하면 생성을 거절한다`() {
        `when`(
            meetingService.createFixedMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "만남",
                location = null,
                participantMemberIds = listOf(99L),
                confirmedDate = LocalDate.of(2026, 10, 3),
            ),
        ).thenThrow(BusinessException(MeetingErrorCode.INVALID_PARTICIPANT))

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","participantMemberIds":[99],"schedule":{"type":"FIXED","date":"2026-10-03"}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEETING-E001"))
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList())
}
