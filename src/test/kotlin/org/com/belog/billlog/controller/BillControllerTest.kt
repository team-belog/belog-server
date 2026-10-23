package org.com.belog.billlog.controller

import org.com.belog.billlog.domain.BillCurrency
import org.com.belog.billlog.domain.BillSplitType
import org.com.belog.billlog.service.BillLogService
import org.com.belog.billlog.service.BillService
import org.com.belog.billlog.service.command.BillItemCommand
import org.com.belog.billlog.service.command.BillShareCommand
import org.com.belog.billlog.service.command.RegisterBillCommand
import org.com.belog.billlog.service.result.RegisteredBill
import org.junit.jupiter.api.Test
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
import java.math.BigDecimal

@WebMvcTest(BillController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BillControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockitoBean
    private lateinit var billService: BillService

    @MockitoBean
    private lateinit var billLogService: BillLogService

    @Test
    fun `화폐 단위를 생략하면 원화로 결제 내역을 등록한다`() {
        val expectedCommand =
            RegisterBillCommand(
                meetingId = 7L,
                creatorUserId = 15L,
                title = "아랑이 카페",
                payerMemberId = 10L,
                currency = BillCurrency.KRW,
                totalAmount = BigDecimal("11000"),
                splitType = BillSplitType.EQUAL_SPLIT,
                items = listOf(BillItemCommand(name = "아메리카노", amount = BigDecimal("11000"))),
                shares = listOf(BillShareCommand(participantMemberId = 10L, amount = BigDecimal("11000"))),
            )
        `when`(billService.registerBill(expectedCommand)).thenReturn(RegisteredBill(billId = 1L))

        mockMvc
            .perform(registerBillRequestWithoutCurrency())
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.data.billId").value(1))
    }

    private fun registerBillRequestWithoutCurrency() =
        post("/api/v1/meetings/7/bill-log/bills")
            .principal(UsernamePasswordAuthenticationToken.authenticated("15", null, emptyList()))
            .contentType(MediaType.APPLICATION_JSON)
            .content(
                """
                {"title":"아랑이 카페","payerMemberId":10,"totalAmount":11000,"splitType":"EQUAL_SPLIT",
                "items":[{"name":"아메리카노","amount":11000}],"shares":[{"participantMemberId":10,"amount":11000}]}
                """.trimIndent(),
            )
}
