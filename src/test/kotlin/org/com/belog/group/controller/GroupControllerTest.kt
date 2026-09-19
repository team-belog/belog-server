package org.com.belog.group.controller

import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.com.belog.group.domain.GroupCoverImageUpload
import org.com.belog.group.service.GroupCoverImageService
import org.com.belog.group.service.GroupService
import org.com.belog.group.service.result.CreatedGroup
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
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
import kotlin.test.assertEquals

@WebMvcTest(GroupController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class GroupControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var groupService: GroupService

    @MockitoBean
    private lateinit var groupCoverImageService: GroupCoverImageService

    @Test
    fun `그룹 커버 이미지 업로드 URL을 발급한다`() {
        val upload =
            GroupCoverImageUpload(
                objectKey = "group-covers/15/image-id.webp",
                uploadUrl = "https://belog-test-storage.s3.ap-northeast-2.amazonaws.com/upload",
                contentType = "image/webp",
                contentLength = 524_288L,
                expiresAt = Instant.parse("2026-09-17T03:05:00Z"),
            )
        `when`(groupCoverImageService.issueUploadUrl(15L, "image/webp", 524_288L)).thenReturn(upload)

        mockMvc
            .perform(
                post("/api/v1/groups/cover-image/upload-url")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                        {
                          "contentType": "image/webp",
                          "fileSize": 524288
                        }
                        """.trimIndent(),
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("GROUP-S002"))
            .andExpect(jsonPath("$.message").value("그룹 커버 이미지 업로드 URL이 발급되었습니다."))
            .andExpect(jsonPath("$.data.objectKey").value("group-covers/15/image-id.webp"))
            .andExpect(jsonPath("$.data.uploadUrl").value(upload.uploadUrl))
            .andExpect(jsonPath("$.data.method").value("PUT"))
            .andExpect(jsonPath("$.data.requiredHeaders.Content-Type").value("image/webp"))
            .andExpect(jsonPath("$.data.requiredHeaders.Content-Length").value("524288"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-09-17T03:05:00Z"))
    }

    @Test
    fun `그룹 커버 이미지 업로드 요청값이 올바르지 않으면 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups/cover-image/upload-url")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"contentType":" ","fileSize":0}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(groupCoverImageService)
    }

    @Test
    fun `지원하지 않는 그룹 커버 이미지 형식이면 400 응답을 반환한다`() {
        `when`(groupCoverImageService.issueUploadUrl(15L, "image/gif", 1024L))
            .thenThrow(BusinessException(GroupErrorCode.UNSUPPORTED_COVER_IMAGE_TYPE))

        mockMvc
            .perform(
                post("/api/v1/groups/cover-image/upload-url")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"contentType":"image/gif","fileSize":1024}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GROUP-E004"))
    }

    @Test
    fun `커버 이미지 없이 그룹을 생성한다`() {
        `when`(groupService.createGroup(15L, "주말 러닝 모임", null))
            .thenReturn(createdGroup())

        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"주말 러닝 모임"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.code").value("GROUP-S001"))
            .andExpect(jsonPath("$.message").value("그룹이 생성되었습니다."))
            .andExpect(jsonPath("$.data.groupId").value(1))
            .andExpect(jsonPath("$.data.name").value("주말 러닝 모임"))
            .andExpect(jsonPath("$.data.coverImageUrl").doesNotExist())
            .andExpect(jsonPath("$.data.currentMemberCount").value(1))
            .andExpect(jsonPath("$.data.inviteCode").value("AB12CD"))
            .andExpect(jsonPath("$.data.inviteLink").value("https://belog.co.kr/invitations/AB12CD"))
    }

    @Test
    fun `현재 사용자의 커버 이미지 Object Key로 그룹을 생성한다`() {
        val objectKey = "group-covers/15/550e8400-e29b-41d4-a716-446655440000.webp"
        val coverImage = GroupCoverImageObjectKey.create(15L, objectKey)
        `when`(groupService.createGroup(15L, "주말 러닝 모임", coverImage))
            .thenReturn(createdGroup())

        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"주말 러닝 모임","coverImageObjectKey":"$objectKey"}"""),
            ).andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.coverImageUrl").doesNotExist())

        val invocation = mockingDetails(groupService).invocations.single()
        assertEquals(15L, invocation.arguments[0])
        assertEquals("주말 러닝 모임", invocation.arguments[1])
        assertEquals(objectKey, invocation.arguments[2])
    }

    @Test
    fun `다른 사용자의 커버 이미지 Object Key는 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"name":"주말 러닝 모임","coverImageObjectKey":"group-covers/16/image.webp"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("GROUP-E003"))

        verifyNoInteractions(groupService)
    }

    @Test
    fun `그룹명이 공백이거나 20자를 초과하면 거절한다`() {
        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"   "}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"${"가".repeat(21)}"}"""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(groupService)
    }

    @Test
    fun `온보딩을 완료하지 않은 사용자는 403 응답을 반환한다`() {
        `when`(groupService.createGroup(15L, "주말 러닝 모임", null))
            .thenThrow(BusinessException(GroupErrorCode.ONBOARDING_REQUIRED))

        mockMvc
            .perform(
                post("/api/v1/groups")
                    .principal(authenticatedUser())
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"주말 러닝 모임"}"""),
            ).andExpect(status().isForbidden)
            .andExpect(jsonPath("$.code").value("GROUP-E001"))
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
                post("/api/v1/groups")
                    .principal(invalidAuthentication)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"name":"주말 러닝 모임"}"""),
            ).andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.code").value("CMN-E005"))

        verifyNoInteractions(groupService)
    }

    private fun authenticatedUser() =
        UsernamePasswordAuthenticationToken.authenticated(
            "15",
            "access-token",
            emptyList(),
        )

    private fun createdGroup(): CreatedGroup =
        CreatedGroup(
            groupId = 1L,
            name = "주말 러닝 모임",
            currentMemberCount = 1,
            inviteCode = "AB12CD",
            inviteLink = "https://belog.co.kr/invitations/AB12CD",
        )
}
