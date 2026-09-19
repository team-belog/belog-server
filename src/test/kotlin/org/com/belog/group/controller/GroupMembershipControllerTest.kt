package org.com.belog.group.controller

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.service.GroupMembershipService
import org.com.belog.group.service.result.JoinedGroup
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

@WebMvcTest(GroupMembershipController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GroupMembershipControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var groupMembershipService: GroupMembershipService

    @Test
    fun `초대 코드로 그룹에 참여한다`() {
        `when`(groupMembershipService.joinGroup(15L, "AB12CD"))
            .thenReturn(
                JoinedGroup(
                    groupId = 1L,
                    name = "주말 러닝 모임",
                    currentMemberCount = 8,
                ),
            )

        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"AB12CD"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("GROUP-S003"))
            .andExpect(jsonPath("$.message").value("그룹에 참여했습니다."))
            .andExpect(jsonPath("$.data.groupId").value(1))
            .andExpect(jsonPath("$.data.name").value("주말 러닝 모임"))
            .andExpect(jsonPath("$.data.currentMemberCount").value(8))
    }

    @Test
    fun `초대 코드가 비어 있거나 6자리가 아니면 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":" "}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"ABC12"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(groupMembershipService)
    }

    @Test
    fun `존재하지 않는 초대 코드면 404 응답을 반환한다`() {
        `when`(groupMembershipService.joinGroup(15L, "AB12CD"))
            .thenThrow(BusinessException(GroupErrorCode.GROUP_NOT_FOUND_BY_INVITE_CODE))

        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"AB12CD"}"""),
            ).andExpect(status().isNotFound)
            .andExpect(jsonPath("$.code").value("GROUP-E009"))
    }

    @Test
    fun `이미 참여한 그룹이면 409 응답을 반환한다`() {
        `when`(groupMembershipService.joinGroup(15L, "AB12CD"))
            .thenThrow(BusinessException(GroupErrorCode.ALREADY_GROUP_MEMBER))

        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"AB12CD"}"""),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("GROUP-E010"))
    }

    @Test
    fun `인증 사용자 식별자가 올바르지 않으면 401 응답을 반환한다`() {
        val invalidAuthentication =
            UsernamePasswordAuthenticationToken.authenticated(
                "invalid-user-id",
                "access-token",
                emptyList(),
            )

        mockMvc
            .perform(
                post("/api/v1/group-memberships")
                    .principal(invalidAuthentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"inviteCode":"AB12CD"}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CMN-E005"))

        verifyNoInteractions(groupMembershipService)
    }

    private fun authenticatedUser() =
        UsernamePasswordAuthenticationToken.authenticated(
            "15",
            "access-token",
            emptyList(),
        )
}
