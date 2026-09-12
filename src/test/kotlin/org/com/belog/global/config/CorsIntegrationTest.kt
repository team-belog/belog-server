package org.com.belog.global.config

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class CorsIntegrationTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `허용된 로컬 Origin의 credential 요청을 허용한다`() {
        mockMvc
            .perform(
                options("/api/v1/auth/refresh")
                    .header(HttpHeaders.ORIGIN, ALLOWED_ORIGIN)
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"),
            ).andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ALLOWED_ORIGIN))
            .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"))
    }

    @Test
    fun `허용되지 않은 Origin의 preflight 요청을 거부한다`() {
        mockMvc
            .perform(
                options("/api/v1/auth/refresh")
                    .header(HttpHeaders.ORIGIN, "https://attacker.example")
                    .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"),
            ).andExpect(status().isForbidden)
            .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
    }

    companion object {
        private const val ALLOWED_ORIGIN = "http://localhost:3000"
    }
}
