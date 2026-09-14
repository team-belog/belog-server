package org.com.belog.user.controller

import org.com.belog.user.domain.Bank
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(BankController::class)
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class BankControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `서버가 지원하는 은행 목록을 반환한다`() {
        mockMvc
            .perform(get("/api/v1/banks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("CMN-S001"))
            .andExpect(jsonPath("$.data.length()").value(Bank.entries.size))
            .andExpect(jsonPath("$.data[0].code").value("KDB"))
            .andExpect(jsonPath("$.data[0].displayName").value("한국산업은행"))
            .andExpect(jsonPath("$.data[4].code").value("NH_NONGHYUP_BANK"))
            .andExpect(jsonPath("$.data[5].code").value("LOCAL_NONGHYUP"))
            .andExpect(jsonPath("$.data[9].code").value("IM_BANK"))
    }
}
