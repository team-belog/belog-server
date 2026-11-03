package org.com.belog.user.controller

import org.com.belog.user.domain.Bank
import org.hamcrest.Matchers.contains
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
    fun `은행 목록을 한글 이름 가나다순 뒤에 영문 이름 순으로 반환한다`() {
        mockMvc
            .perform(get("/api/v1/banks"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.code").value("CMN-S001"))
            .andExpect(jsonPath("$.data.length()").value(Bank.entries.size))
            .andExpect(jsonPath("$.data[0].code").value("GYEONGNAM"))
            .andExpect(
                jsonPath("$.data[*].displayName").value(
                    contains(
                        "경남은행",
                        "광주은행",
                        "부산은행",
                        "새마을금고",
                        "신한은행",
                        "신협",
                        "우리은행",
                        "우체국",
                        "저축은행",
                        "전북은행",
                        "제주은행",
                        "지역 농·축협",
                        "카카오뱅크",
                        "케이뱅크",
                        "토스뱅크",
                        "하나은행",
                        "한국산업은행",
                        "한국씨티은행",
                        "IBK기업은행",
                        "iM뱅크",
                        "KB국민은행",
                        "NH농협은행",
                        "SC제일은행",
                        "Sh수협은행",
                    ),
                ),
            )
    }
}
