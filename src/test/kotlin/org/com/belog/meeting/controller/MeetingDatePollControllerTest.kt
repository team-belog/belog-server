package org.com.belog.meeting.controller

import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.MeetingDatePollService
import org.com.belog.meeting.service.result.CandidateDateRangeResult
import org.com.belog.meeting.service.result.MeetingDatePollResult
import org.com.belog.meeting.service.result.MyDatePollResponseResult
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verify
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.LocalDate

@WebMvcTest(MeetingDatePollController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class MeetingDatePollControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var meetingDatePollService: MeetingDatePollService

    @Test
    fun `후보 일정과 내 응답 상태를 조회한다`() {
        `when`(meetingDatePollService.getDatePoll(meetingId = 7L, userId = 15L))
            .thenReturn(
                MeetingDatePollResult(
                    meetingId = 7L,
                    status = MeetingStatus.SCHEDULING,
                    candidateDateRanges =
                        listOf(
                            CandidateDateRangeResult(
                                id = 101L,
                                startDate = LocalDate.of(2026, 10, 3),
                                endDate = LocalDate.of(2026, 10, 4),
                            ),
                            CandidateDateRangeResult(
                                id = 103L,
                                startDate = LocalDate.of(2026, 10, 10),
                                endDate = LocalDate.of(2026, 10, 11),
                            ),
                        ),
                    myResponse =
                        MyDatePollResponseResult(
                            responded = true,
                            respondedAt = Instant.parse("2026-09-22T00:00:00Z"),
                            selectedCandidateDateRangeIds = listOf(101L, 103L),
                        ),
                ),
            )

        mockMvc
            .perform(
                get("/api/v1/meetings/7/date-poll")
                    .principal(authenticatedUser()),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MEETING-S002"))
            .andExpect(jsonPath("$.data.meetingId").value(7))
            .andExpect(jsonPath("$.data.status").value("SCHEDULING"))
            .andExpect(jsonPath("$.data.candidateDateRanges[0].id").value(101))
            .andExpect(jsonPath("$.data.candidateDateRanges[0].startDate").value("2026-10-03"))
            .andExpect(jsonPath("$.data.candidateDateRanges[1].id").value(103))
            .andExpect(jsonPath("$.data.myResponse.responded").value(true))
            .andExpect(jsonPath("$.data.myResponse.respondedAt").value("2026-09-22T00:00:00Z"))
            .andExpect(jsonPath("$.data.myResponse.selectedCandidateDateRangeIds[0]").value(101))
    }

    @Test
    fun `복수 후보 일정을 선택해 응답한다`() {
        mockMvc
            .perform(
                put("/api/v1/meetings/7/date-poll/responses/me")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"candidateDateRangeIds":[101,103]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MEETING-S003"))
            .andExpect(jsonPath("$.data").isEmpty)

        verify(meetingDatePollService).respondDatePoll(
            meetingId = 7L,
            userId = 15L,
            candidateDateRangeIds = listOf(101L, 103L),
        )
    }

    @Test
    fun `빈 배열로 모든 후보 일정이 불가능하다고 응답한다`() {
        mockMvc
            .perform(
                put("/api/v1/meetings/7/date-poll/responses/me")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"candidateDateRangeIds":[]}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MEETING-S003"))

        verify(meetingDatePollService).respondDatePoll(
            meetingId = 7L,
            userId = 15L,
            candidateDateRangeIds = emptyList(),
        )
    }

    @Test
    fun `후보 일정 ID가 최대 개수를 초과하면 응답을 거절한다`() {
        val candidateIds = (1L..11L).joinToString(",")

        mockMvc
            .perform(
                put("/api/v1/meetings/7/date-poll/responses/me")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"candidateDateRangeIds":[$candidateIds]}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(meetingDatePollService)
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList())
}
