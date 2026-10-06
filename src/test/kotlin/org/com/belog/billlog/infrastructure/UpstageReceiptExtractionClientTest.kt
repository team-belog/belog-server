package org.com.belog.billlog.infrastructure

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.config.UpstageReceiptExtractionProperties
import org.com.belog.global.error.BusinessException
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.content
import org.springframework.test.web.client.match.MockRestRequestMatchers.header
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withServerError
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess
import org.springframework.web.client.RestClient
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.math.BigDecimal
import java.net.URI
import java.time.Duration
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class UpstageReceiptExtractionClientTest {
    private val restClientBuilder =
        RestClient
            .builder()
            .baseUrl(properties.baseUrl.toString())
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer ${properties.apiKey}")
    private val server = MockRestServiceServer.bindTo(restClientBuilder).build()
    private val client = UpstageReceiptExtractionClient(restClientBuilder.build(), properties, jacksonObjectMapper())

    @Test
    fun `영수증 이미지에서 구조화된 결제 정보를 추출한다`() {
        server
            .expect(requestTo("https://api.upstage.ai/v1/information-extraction/chat/completions"))
            .andExpect(method(HttpMethod.POST))
            .andExpect(header(HttpHeaders.AUTHORIZATION, "Bearer test-upstage-api-key"))
            .andExpect(
                content().string(
                    allOf(
                        containsString("\"model\":\"information-extract\""),
                        containsString("\"mode\":\"standard\""),
                        containsString("\"url\":\"https://receipt.example/image.jpg\""),
                        containsString("\"response_format\""),
                        containsString("\"totalAmount\""),
                        containsString("\"title\":{\"type\":\"string\""),
                        containsString("\"paymentDate\":{\"type\":\"string\""),
                        containsString("\"items\":{\"type\":\"array\""),
                        containsString("\"name\":{\"type\":\"string\""),
                        containsString("\"amount\":{\"type\":\"number\""),
                        containsString("\"totalAmount\":{\"type\":\"number\""),
                        not(containsString("[\"string\",\"null\"]")),
                    ),
                ),
            ).andRespond(
                withSuccess(
                    """
                    {
                      "choices": [
                        {
                          "message": {
                            "content": "{\"title\":\"벨로그 식당\",\"paymentDate\":\"2026-10-06\",\"items\":[{\"name\":\"파스타\",\"amount\":15000}],\"totalAmount\":15000}"
                          }
                        }
                      ]
                    }
                    """.trimIndent(),
                    MediaType.APPLICATION_JSON,
                ),
            )

        val result = client.extract("https://receipt.example/image.jpg")

        assertEquals("벨로그 식당", result.title)
        assertEquals("2026-10-06", result.paymentDate)
        assertEquals("파스타", result.items.single().name)
        assertEquals(BigDecimal("15000"), result.items.single().amount)
        assertEquals(BigDecimal("15000"), result.totalAmount)
        server.verify()
    }

    @Test
    fun `Upstage 응답 시간이 초과되면 타임아웃 오류로 변환한다`() {
        server
            .expect(requestTo("https://api.upstage.ai/v1/information-extraction/chat/completions"))
            .andRespond(withStatus(HttpStatus.GATEWAY_TIMEOUT))

        val exception =
            assertFailsWith<BusinessException> {
                client.extract("https://receipt.example/image.jpg")
            }

        assertEquals(BillLogErrorCode.RECEIPT_ANALYSIS_TIMEOUT, exception.errorCode)
        server.verify()
    }

    @Test
    fun `Upstage 호출이 실패하면 분석 실패 오류로 변환한다`() {
        server
            .expect(requestTo("https://api.upstage.ai/v1/information-extraction/chat/completions"))
            .andRespond(withServerError())

        val exception =
            assertFailsWith<BusinessException> {
                client.extract("https://receipt.example/image.jpg")
            }

        assertEquals(BillLogErrorCode.RECEIPT_ANALYSIS_FAILED, exception.errorCode)
        server.verify()
    }

    companion object {
        private val properties =
            UpstageReceiptExtractionProperties(
                baseUrl = URI.create("https://api.upstage.ai/v1/information-extraction/"),
                apiKey = "test-upstage-api-key",
                model = "information-extract",
                mode = "standard",
                connectionTimeout = Duration.ofSeconds(3),
                readTimeout = Duration.ofSeconds(60),
            )
    }
}
