package org.com.belog.meeting.controller

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MeetingOpenApiTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `미응답자 리마인드 API의 204 응답 문서에는 본문 스키마가 없다`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths['$REMINDER_PATH'].post.responses['204']").exists())
            .andExpect(jsonPath("$.paths['$REMINDER_PATH'].post.responses['204'].content").doesNotExist())
    }

    @Test
    fun `만남 삭제 API의 204 응답 문서에는 본문 스키마가 없다`() {
        mockMvc
            .perform(get("/v3/api-docs"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.paths['$MEETING_PATH'].delete.responses['204']").exists())
            .andExpect(jsonPath("$.paths['$MEETING_PATH'].delete.responses['204'].content").doesNotExist())
    }

    companion object {
        private const val REMINDER_PATH = "/api/v1/meetings/{meetingId}/date-poll/reminders"
        private const val MEETING_PATH = "/api/v1/meetings/{meetingId}"
    }
}
