package org.com.belog.user.controller

import org.com.belog.user.domain.ProfileImageUpload
import org.com.belog.user.service.ProfileImageService
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.time.Instant
import kotlin.test.assertEquals

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var profileImageService: ProfileImageService

    @Test
    fun `다른 사용자의 object key로 온보딩을 요청하면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticationFor(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validOnboardingRequest("users/2/profile/image-key")),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("USER-E004"))

        verifyNoInteractions(userService)
    }

    @Test
    fun `상위 경로 이동이 포함된 object key로 온보딩을 요청하면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticationFor(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validOnboardingRequest("users/1/profile/../image-key")),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("USER-E004"))

        verifyNoInteractions(userService)
    }

    @Test
    fun `현재 사용자의 object key로 온보딩 정보를 저장한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticationFor(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validOnboardingRequest("users/1/profile/image-key")),
            ).andExpect(status().isOk)

        val invocation =
            mockingDetails(userService).invocations.single { invocation ->
                invocation.method.name.startsWith("completeOnboarding")
            }
        assertEquals(1L, invocation.arguments[0])
        assertEquals("users/1/profile/image-key", invocation.arguments[1])
        assertEquals("빌로그", invocation.arguments[2])
        val bankAccount = invocation.arguments[3] as BankAccount
        assertEquals(Bank.SHINHAN, bankAccount.bank)
        assertEquals("110123456789", bankAccount.accountNumber)
        assertEquals("홍길동", bankAccount.accountHolderName)
    }

    @Test
    fun `사용 가능한 닉네임이면 true를 반환한다`() {
        `when`(userService.isNicknameAvailable("김빌로")).thenReturn(true)

        mockMvc
            .perform(
                get("/api/v1/users/nickname/availability")
                    .param("nickname", "김빌로"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("CMN-S001"))
            .andExpect(jsonPath("$.data.nickname").value("김빌로"))
            .andExpect(jsonPath("$.data.available").value(true))
    }

    @Test
    fun `이미 사용 중인 닉네임이면 false를 반환한다`() {
        `when`(userService.isNicknameAvailable("김빌로")).thenReturn(false)

        mockMvc
            .perform(
                get("/api/v1/users/nickname/availability")
                    .param("nickname", "김빌로"),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.available").value(false))
    }

    @Test
    fun `닉네임 앞뒤 공백을 제거한 값으로 중복을 확인한다`() {
        `when`(userService.isNicknameAvailable("김빌로")).thenReturn(true)

        mockMvc
            .perform(
                get("/api/v1/users/nickname/availability")
                    .param("nickname", " 김빌로 "),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.data.nickname").value("김빌로"))
            .andExpect(jsonPath("$.data.available").value(true))

        verify(userService).isNicknameAvailable("김빌로")
    }

    @Test
    fun `닉네임이 비어 있으면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                get("/api/v1/users/nickname/availability")
                    .param("nickname", ""),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))
            .andExpect(jsonPath("$.data.fieldErrors[0].field").value("nickname"))
            .andExpect(jsonPath("$.data.fieldErrors[0].reason").value("닉네임은 1자 이상 8자 이하여야 합니다."))

        verifyNoInteractions(userService)
    }

    @Test
    fun `닉네임이 8자를 초과하면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                get("/api/v1/users/nickname/availability")
                    .param("nickname", "123456789"),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))
            .andExpect(jsonPath("$.data.fieldErrors[0].field").value("nickname"))
            .andExpect(jsonPath("$.data.fieldErrors[0].reason").value("닉네임은 1자 이상 8자 이하여야 합니다."))

        verifyNoInteractions(userService)
    }

    @Test
    fun `프로필 이미지 업로드 URL을 발급한다`() {
        val upload =
            ProfileImageUpload(
                objectKey = "users/15/profile/image-id.webp",
                uploadUrl = "https://belog-test-storage.s3.ap-northeast-2.amazonaws.com/upload",
                contentType = "image/webp",
                contentLength = 524_288L,
                expiresAt = Instant.parse("2026-09-14T14:05:00Z"),
            )
        `when`(profileImageService.issueUploadUrl(15L, "image/webp", 524_288L)).thenReturn(upload)

        mockMvc
            .perform(
                post("/api/v1/users/me/profile-image/upload-url")
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
            .andExpect(jsonPath("$.code").value("USER-S001"))
            .andExpect(jsonPath("$.data.objectKey").value("users/15/profile/image-id.webp"))
            .andExpect(jsonPath("$.data.method").value("PUT"))
            .andExpect(jsonPath("$.data.requiredHeaders.Content-Type").value("image/webp"))
            .andExpect(jsonPath("$.data.requiredHeaders.Content-Length").value("524288"))
            .andExpect(jsonPath("$.data.expiresAt").value("2026-09-14T14:05:00Z"))
    }

    private fun authenticatedUser() =
        UsernamePasswordAuthenticationToken.authenticated(
            "15",
            "access-token",
            emptyList(),
        )

    private fun authenticationFor(userId: Long): JwtAuthenticationToken {
        val now = Instant.parse("2026-09-15T00:00:00Z")
        val jwt =
            Jwt
                .withTokenValue("access-token")
                .header("alg", "HS256")
                .subject(userId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(1800))
                .build()
        return JwtAuthenticationToken(jwt)
    }

    private fun validOnboardingRequest(profileImageObjectKey: String): String =
        """{"profileImageObjectKey":"$profileImageObjectKey","nickname":"빌로그","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}"""
}
