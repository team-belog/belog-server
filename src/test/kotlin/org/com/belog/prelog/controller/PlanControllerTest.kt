package org.com.belog.prelog.controller

import org.com.belog.meeting.domain.Meeting
import org.com.belog.meeting.domain.MeetingParticipant
import org.com.belog.prelog.domain.Plan
import org.com.belog.prelog.domain.PlanCategory
import org.com.belog.prelog.domain.PlanType
import org.com.belog.prelog.service.PlanService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
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

@WebMvcTest(PlanController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PlanControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var planService: PlanService

    @Test
    fun `LINK 계획을 생성한다`() {
        val plan = mockLinkPlan()
        `when`(
            planService.createLinkPlan(
                meetingId = 1L,
                creatorUserId = 15L,
                category = PlanCategory.RESTAURANT,
                title = "광주 맛집",
                url = "https://example.com/place",
            ),
        ).thenReturn(plan)

        mockMvc
            .perform(
                post("/api/v1/meetings/1/pre-log/plans")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"LINK","category":"RESTAURANT","title":"광주 맛집","url":"https://example.com/place"}""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("PRE_LOG-S001"))
            .andExpect(jsonPath("$.message").value("계획이 생성되었습니다."))
            .andExpect(jsonPath("$.data.planId").value(7))
            .andExpect(jsonPath("$.data.meetingId").value(1))
            .andExpect(jsonPath("$.data.creatorParticipantId").value(10))
            .andExpect(jsonPath("$.data.type").value("LINK"))
            .andExpect(jsonPath("$.data.category").value("RESTAURANT"))
            .andExpect(jsonPath("$.data.title").value("광주 맛집"))
            .andExpect(jsonPath("$.data.url").value("https://example.com/place"))
            .andExpect(jsonPath("$.data.content").isEmpty)
    }

    @Test
    fun `유형과 일치하지 않는 필드를 전달하면 계획 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/meetings/1/pre-log/plans")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"LINK","category":"RESTAURANT","title":"맛집","content":"메모"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        mockMvc
            .perform(
                post("/api/v1/meetings/1/pre-log/plans")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"type":"MEMO","category":"RESTAURANT","title":"맛집","url":"https://example.com/place"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(planService)
    }

    private fun mockLinkPlan(): Plan {
        val meeting = mock(Meeting::class.java)
        val participant = mock(MeetingParticipant::class.java)
        val plan = mock(Plan::class.java)
        `when`(meeting.id).thenReturn(1L)
        `when`(participant.id).thenReturn(10L)
        `when`(plan.id).thenReturn(7L)
        `when`(plan.meeting).thenReturn(meeting)
        `when`(plan.createdBy).thenReturn(participant)
        `when`(plan.type).thenReturn(PlanType.LINK)
        `when`(plan.category).thenReturn(PlanCategory.RESTAURANT)
        `when`(plan.title).thenReturn("광주 맛집")
        `when`(plan.url).thenReturn("https://example.com/place")
        `when`(plan.content).thenReturn(null)
        return plan
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList())
}
