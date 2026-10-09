package org.com.belog.billlog.controller

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.service.BillLogService
import org.com.belog.billlog.service.SettlementRequestService
import org.com.belog.global.error.BusinessException
import org.junit.jupiter.api.Test
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SettlementRequestController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SettlementRequestControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var billLogService: BillLogService

    @MockitoBean
    private lateinit var settlementRequestService: SettlementRequestService

    @Test
    fun `정산 리마인드를 전송하면 본문 없이 204를 반환한다`() {
        mockMvc
            .perform(
                post("/api/v1/settlement-requests/12/reminders")
                    .principal(authenticatedUser()),
            ).andExpect(status().isNoContent)
            .andExpect(content().string(""))

        verify(settlementRequestService).remind(settlementRequestId = 12L, requesterUserId = 15L)
    }

    @Test
    fun `마지막 리마인드 후 1분이 지나지 않으면 429를 반환한다`() {
        doThrow(BusinessException(BillLogErrorCode.SETTLEMENT_REMINDER_TOO_FREQUENT))
            .`when`(settlementRequestService)
            .remind(settlementRequestId = 12L, requesterUserId = 15L)

        mockMvc
            .perform(
                post("/api/v1/settlement-requests/12/reminders")
                    .principal(authenticatedUser()),
            ).andExpect(status().isTooManyRequests)
            .andExpect(jsonPath("$.code").value("BILL_LOG-E027"))
            .andExpect(jsonPath("$.message").value("정산 리마인드는 1분 후에 다시 보낼 수 있습니다."))
    }

    private fun authenticatedUser(): UsernamePasswordAuthenticationToken =
        UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList())
}
