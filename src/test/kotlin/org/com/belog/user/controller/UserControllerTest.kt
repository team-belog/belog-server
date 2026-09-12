package org.com.belog.user.controller

import org.com.belog.user.service.UserService
import org.junit.jupiter.api.Test
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(UserController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class UserControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var userService: UserService

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
}
