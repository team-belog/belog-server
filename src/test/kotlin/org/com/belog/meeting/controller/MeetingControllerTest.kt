package org.com.belog.meeting.controller

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.meeting.code.MeetingErrorCode
import org.com.belog.meeting.domain.MeetingDateRange
import org.com.belog.meeting.domain.MeetingScheduleType
import org.com.belog.meeting.domain.MeetingStatus
import org.com.belog.meeting.service.MeetingDetailService
import org.com.belog.meeting.service.MeetingMemberService
import org.com.belog.meeting.service.MeetingService
import org.com.belog.meeting.service.result.CreatedMeeting
import org.com.belog.meeting.service.result.MeetingMemberResult
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
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

    @MockitoBean
    private lateinit var meetingDetailService: MeetingDetailService

    @MockitoBean
    private lateinit var meetingMemberService: MeetingMemberService

    @Test
    fun `만남 멤버 목록은 서비스의 순서와 그룹 멤버 ID 및 본인 여부를 반환한다`() {
        `when`(meetingMemberService.getMeetingMembers(7L, 15L)).thenReturn(
            listOf(
                MeetingMemberResult(22L, "요청자", null, true),
                MeetingMemberResult(21L, "참여자", "https://example.com/profile", false),
            ),
        )

        mockMvc
            .perform(get("/api/v1/meetings/7/members").principal(authenticatedUser()))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("MEETING-S011"))
            .andExpect(jsonPath("$.message").value("만남 멤버 목록을 조회했습니다."))
            .andExpect(jsonPath("$.data.items.length()").value(2))
            .andExpect(jsonPath("$.data.items[0].groupMemberId").value(22))
            .andExpect(jsonPath("$.data.items[0].nickname").value("요청자"))
            .andExpect(jsonPath("$.data.items[0].profileImageUrl").isEmpty)
            .andExpect(jsonPath("$.data.items[0].isMe").value(true))
            .andExpect(jsonPath("$.data.items[1].groupMemberId").value(21))
            .andExpect(jsonPath("$.data.items[1].nickname").value("참여자"))
            .andExpect(jsonPath("$.data.items[1].profileImageUrl").value("https://example.com/profile"))
            .andExpect(jsonPath("$.data.items[1].isMe").value(false))
    }

    @Test
    fun `만남 멤버 조회는 인증 정보가 없으면 거부한다`() {
        mockMvc
            .perform(get("/api/v1/meetings/7/members"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CMN-E005"))
    }

    @Test
    fun `만남 멤버 조회는 그룹 멤버가 아니면 403을 반환한다`() {
        `when`(meetingMemberService.getMeetingMembers(7L, 15L))
            .thenThrow(BusinessException(GroupErrorCode.NOT_GROUP_MEMBER))

        mockMvc
            .perform(get("/api/v1/meetings/7/members").principal(authenticatedUser()))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GROUP-E013"))
    }

    @Test
    fun `만남 멤버 조회는 조회할 만남이 없으면 404를 반환한다`() {
        `when`(meetingMemberService.getMeetingMembers(7L, 15L))
            .thenThrow(BusinessException(MeetingErrorCode.MEETING_NOT_FOUND))

        mockMvc
            .perform(get("/api/v1/meetings/7/members").principal(authenticatedUser()))
            .andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("MEETING-E010"))
    }

    @Test
    fun `확정 날짜 만남을 생성한다`() {
        `when`(
            meetingService.createFixedMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "광주 1박 2일",
                location = "서울고속버스터미널",
                participantMemberIds = listOf(22L, 23L),
                startDate = LocalDate.of(2026, 10, 3),
                endDate = LocalDate.of(2026, 10, 4),
            ),
        ).thenReturn(
            CreatedMeeting(
                meetingId = 7L,
                groupId = 1L,
                name = "광주 1박 2일",
                location = "서울고속버스터미널",
                scheduleType = MeetingScheduleType.FIXED,
                status = MeetingStatus.CONFIRMED,
                startDate = LocalDate.of(2026, 10, 3),
                endDate = LocalDate.of(2026, 10, 4),
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
                            "dateRanges": [
                              {
                                "startDate": "2026-10-03",
                                "endDate": "2026-10-04"
                              }
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("MEETING-S001"))
            .andExpect(jsonPath("$.message").value("만남이 생성되었습니다."))
            .andExpect(jsonPath("$.data.meetingId").value(7))
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `만남명이 공백이면 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":" ","schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"}]}}""",
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
                        """{"name":"만남","participantMemberIds":[$participantIds],"schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"}]}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `후보 일정 범위로 일정 조율 만남을 생성한다`() {
        `when`(
            meetingService.createPollMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "만남",
                location = null,
                participantMemberIds = emptyList(),
                candidateDateRanges =
                    listOf(
                        MeetingDateRange(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 4)),
                        MeetingDateRange(LocalDate.of(2026, 10, 10), LocalDate.of(2026, 10, 11)),
                    ),
            ),
        ).thenReturn(
            CreatedMeeting(
                meetingId = 7L,
                groupId = 1L,
                name = "만남",
                location = null,
                scheduleType = MeetingScheduleType.POLL,
                status = MeetingStatus.SCHEDULING,
                startDate = null,
                endDate = null,
                confirmedAt = null,
                participantCount = 1,
            ),
        )

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"POLL","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"},{"startDate":"2026-10-10","endDate":"2026-10-11"}]}}""",
                    ),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("MEETING-S001"))
            .andExpect(jsonPath("$.data.meetingId").value(7))
            .andExpect(jsonPath("$.data.length()").value(1))
    }

    @Test
    fun `확정 날짜 방식에 일정 범위를 두 개 전달하면 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "만남",
                          "schedule": {
                            "type": "FIXED",
                            "dateRanges": [
                              {"startDate": "2026-10-03", "endDate": "2026-10-04"},
                              {"startDate": "2026-10-10", "endDate": "2026-10-11"}
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `일정 조율 방식에 후보 일정 범위를 한 개만 전달하면 생성을 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "name": "만남",
                          "schedule": {
                            "type": "POLL",
                            "dateRanges": [
                              {"startDate": "2026-10-03", "endDate": "2026-10-04"}
                            ]
                          }
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

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
                        """{"name":"만남","schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026.10.03","endDate":"2026-10-04"}]}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E002"))

        verifyNoInteractions(meetingService)
    }

    @Test
    fun `종료일이 시작일보다 빠르면 생성을 거절한다`() {
        `when`(
            meetingService.createFixedMeeting(
                groupId = 1L,
                creatorUserId = 15L,
                name = "만남",
                location = null,
                participantMemberIds = emptyList(),
                startDate = LocalDate.of(2026, 10, 4),
                endDate = LocalDate.of(2026, 10, 3),
            ),
        ).thenThrow(BusinessException(MeetingErrorCode.INVALID_MEETING_DATE_RANGE))

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-04","endDate":"2026-10-03"}]}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEETING-E007"))
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
                startDate = LocalDate.of(2026, 10, 3),
                endDate = LocalDate.of(2026, 10, 4),
            ),
        ).thenThrow(BusinessException(GroupErrorCode.NOT_GROUP_MEMBER))

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"}]}}""",
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
                startDate = LocalDate.of(2026, 10, 3),
                endDate = LocalDate.of(2026, 10, 4),
            ),
        ).thenThrow(BusinessException(MeetingErrorCode.INVALID_PARTICIPANT))

        mockMvc
            .perform(
                post("/api/v1/groups/1/meetings")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"만남","participantMemberIds":[99],"schedule":{"type":"FIXED","dateRanges":[{"startDate":"2026-10-03","endDate":"2026-10-04"}]}}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("MEETING-E001"))
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList())
}
