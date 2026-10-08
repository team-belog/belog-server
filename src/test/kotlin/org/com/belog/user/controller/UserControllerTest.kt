package org.com.belog.user.controller

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.global.error.BusinessException
import org.com.belog.postlog.service.PostLogCalendarService
import org.com.belog.user.code.UserErrorCode
import org.com.belog.user.domain.Bank
import org.com.belog.user.domain.BankAccount
import org.com.belog.user.domain.ProfileImageUpload
import org.com.belog.user.service.UserService
import org.com.belog.user.service.UserWithdrawalService
import org.com.belog.user.service.command.ProfileImageChange
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
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

    @MockitoBean
    private lateinit var userWithdrawalService: UserWithdrawalService

    @MockitoBean
    private lateinit var postLogCalendarService: PostLogCalendarService

    @Test
    fun `인증된 사용자를 탈퇴 처리하고 204를 반환한다`() {
        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .principal(authenticatedUser(15L)),
            ).andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(userWithdrawalService).withdraw(15L)
    }

    @Test
    fun `완료되지 않은 정산 요청이 있으면 탈퇴를 거절한다`() {
        `when`(userWithdrawalService.withdraw(15L))
            .thenThrow(BusinessException(BillLogErrorCode.UNSETTLED_SETTLEMENT_REQUEST_EXISTS))

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .principal(authenticatedUser(15L)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("BILL_LOG-E023"))
    }

    @Test
    fun `이미 탈퇴한 사용자는 다시 탈퇴할 수 없다`() {
        `when`(userWithdrawalService.withdraw(15L)).thenThrow(BusinessException(UserErrorCode.ALREADY_WITHDRAWN))

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .principal(authenticatedUser(15L)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("USER-E011"))
    }

    @Test
    fun `탈퇴 처리 중 충돌이 발생하면 409를 반환한다`() {
        `when`(userWithdrawalService.withdraw(15L)).thenThrow(BusinessException(UserErrorCode.WITHDRAWAL_CONFLICT))

        mockMvc
            .perform(
                delete("/api/v1/users/me")
                    .principal(authenticatedUser(15L)),
            ).andExpect(status().isConflict)
            .andExpect(jsonPath("$.code").value("USER-E012"))
    }

    @Test
    fun `닉네임만 수정하면 기존 프로필 이미지를 유지하도록 요청한다`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me/profile")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"nickname":"새닉네임"}"""),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("CMN-S001"))

        val invocation = profileUpdateInvocation()
        assertEquals(15L, invocation.arguments[0])
        assertEquals("새닉네임", invocation.arguments[1])
        assertEquals(null, invocation.arguments[2])
    }

    @Test
    fun `업로드한 이미지로 프로필 이미지를 수정하도록 요청한다`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me/profile")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"profileImageType":"CUSTOM","profileImageObjectKey":"users/15/profile/image.webp"}""",
                    ),
            ).andExpect(status().isOk)

        val profileImageChange = assertIs<ProfileImageChange.Update>(profileUpdateInvocation().arguments[2])
        assertEquals("users/15/profile/image.webp", profileImageChange.objectKey.value)
    }

    @Test
    fun `프로필 이미지를 앱 기본 이미지로 수정하도록 요청한다`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me/profile")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""{"profileImageType":"DEFAULT"}"""),
            ).andExpect(status().isOk)

        assertEquals(ProfileImageChange.Reset, profileUpdateInvocation().arguments[2])
    }

    @Test
    fun `수정할 항목이 없는 프로필 수정 요청을 거절한다`() {
        assertInvalidProfileUpdate("{}")
    }

    @Test
    fun `CUSTOM 타입에 object key가 없는 프로필 수정 요청을 거절한다`() {
        assertInvalidProfileUpdate("""{"profileImageType":"CUSTOM"}""")
    }

    @Test
    fun `DEFAULT 타입에 object key가 포함된 프로필 수정 요청을 거절한다`() {
        assertInvalidProfileUpdate(
            """{"profileImageType":"DEFAULT","profileImageObjectKey":"users/15/profile/image.webp"}""",
        )
    }

    @Test
    fun `다른 사용자의 object key로 프로필 수정을 요청하면 거절한다`() {
        mockMvc
            .perform(
                patch("/api/v1/users/me/profile")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"profileImageType":"CUSTOM","profileImageObjectKey":"users/20/profile/image.webp"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("USER-E004"))

        verifyNoInteractions(userService)
    }

    @Test
    fun `인증된 사용자의 계좌 정보를 수정한다`() {
        mockMvc
            .perform(
                put("/api/v1/users/me/bank-account")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}""",
                    ),
            ).andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("CMN-S001"))

        val invocation =
            mockingDetails(userService).invocations.single { invocation ->
                invocation.method.name.startsWith("updateBankAccount")
            }
        assertEquals(15L, invocation.arguments[0])
        val bankAccount = invocation.arguments[1] as BankAccount
        assertEquals(Bank.SHINHAN, bankAccount.bank)
        assertEquals("110123456789", bankAccount.accountNumber)
        assertEquals("홍길동", bankAccount.accountHolderName)
    }

    @Test
    fun `잘못된 계좌 수정 요청을 거절한다`() {
        mockMvc
            .perform(
                put("/api/v1/users/me/bank-account")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"bankCode":"SHINHAN","accountNumber":"110-ABC-456789","accountHolderName":"홍길동"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))
            .andExpect(jsonPath("$.data.fieldErrors[0].field").value("accountNumber"))

        verifyNoInteractions(userService)
    }

    @Test
    fun `다른 사용자의 object key로 온보딩을 요청하면 400 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticatedUser(1L))
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
                    .principal(authenticatedUser(1L))
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
                    .principal(authenticatedUser(1L))
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
        assertEquals("홍길동", invocation.arguments[3])
        val bankAccount = invocation.arguments[4] as BankAccount
        assertEquals(Bank.SHINHAN, bankAccount.bank)
        assertEquals("110123456789", bankAccount.accountNumber)
        assertEquals("홍길동", bankAccount.accountHolderName)
    }

    @Test
    fun `프로필 이미지 object key 없이 온보딩 정보를 저장한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticatedUser(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"nickname":"빌로그","name":"홍길동","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}""",
                    ),
            ).andExpect(status().isOk)

        val invocation =
            mockingDetails(userService).invocations.single { invocation ->
                invocation.method.name.startsWith("completeOnboarding")
            }
        assertEquals(null, invocation.arguments[1])
    }

    @Test
    fun `사용자 이름이 누락된 온보딩 요청은 400 응답을 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/users/me/onboarding")
                    .principal(authenticatedUser(1L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """{"nickname":"빌로그","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}""",
                    ),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E002"))

        verifyNoInteractions(userService)
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
        `when`(userService.issueProfileImageUploadUrl(15L, "image/webp", 524_288L)).thenReturn(upload)

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

    private fun authenticatedUser(userId: Long = 15L) =
        UsernamePasswordAuthenticationToken.authenticated(
            userId.toString(),
            "access-token",
            emptyList(),
        )

    private fun profileUpdateInvocation() =
        mockingDetails(userService).invocations.single { invocation ->
            invocation.method.name.startsWith("updateProfile")
        }

    private fun assertInvalidProfileUpdate(content: String) {
        mockMvc
            .perform(
                patch("/api/v1/users/me/profile")
                    .principal(authenticatedUser(15L))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(content),
            ).andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.code").value("CMN-E001"))

        verifyNoInteractions(userService)
    }

    private fun validOnboardingRequest(profileImageObjectKey: String): String =
        """{"profileImageObjectKey":"$profileImageObjectKey","nickname":"빌로그","name":"홍길동","bankCode":"SHINHAN","accountNumber":"110123456789","accountHolderName":"홍길동"}"""
}
