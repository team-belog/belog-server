package org.com.belog.billlog.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.config.UpstageReceiptExtractionConfig
import org.com.belog.billlog.config.UpstageReceiptExtractionProperties
import org.com.belog.billlog.domain.ReceiptExtraction
import org.com.belog.global.error.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.core.JacksonException
import tools.jackson.databind.ObjectMapper
import java.net.SocketTimeoutException

@Component
class UpstageReceiptExtractionClient(
    @Qualifier(UpstageReceiptExtractionConfig.UPSTAGE_RECEIPT_EXTRACTION_REST_CLIENT)
    private val restClient: RestClient,
    private val properties: UpstageReceiptExtractionProperties,
    private val objectMapper: ObjectMapper,
) {
    fun extract(readUrl: String): ReceiptExtraction {
        val response = requestExtraction(readUrl)
        val content =
            response.choices
                .firstOrNull()
                ?.message
                ?.content
                ?: throw BusinessException(BillLogErrorCode.RECEIPT_ANALYSIS_FAILED)

        return try {
            objectMapper.readValue(content, ReceiptExtraction::class.java)
        } catch (exception: JacksonException) {
            log.warn("Upstage receipt extraction returned an invalid structured response")
            throw BusinessException(BillLogErrorCode.RECEIPT_ANALYSIS_FAILED, exception)
        }
    }

    private fun requestExtraction(readUrl: String): UpstageExtractionResponse {
        val request =
            UpstageExtractionRequest(
                model = properties.model,
                messages =
                    listOf(
                        UpstageMessage(
                            role = USER_ROLE,
                            content =
                                listOf(
                                    UpstageImageContent(
                                        type = IMAGE_URL_TYPE,
                                        imageUrl = UpstageImageUrl(readUrl),
                                    ),
                                ),
                        ),
                    ),
                responseFormat = RECEIPT_RESPONSE_FORMAT,
                mode = properties.mode,
            )

        try {
            return restClient
                .post()
                .uri(CHAT_COMPLETIONS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(UpstageExtractionResponse::class.java)
                ?: throw BusinessException(BillLogErrorCode.RECEIPT_ANALYSIS_FAILED)
        } catch (exception: RestClientResponseException) {
            val errorCode =
                if (exception.statusCode == HttpStatus.REQUEST_TIMEOUT || exception.statusCode == HttpStatus.GATEWAY_TIMEOUT) {
                    BillLogErrorCode.RECEIPT_ANALYSIS_TIMEOUT
                } else {
                    BillLogErrorCode.RECEIPT_ANALYSIS_FAILED
                }
            log.warn("Upstage receipt extraction request failed: status={}", exception.statusCode)
            throw BusinessException(errorCode, exception)
        } catch (exception: RestClientException) {
            val errorCode =
                if (exception.hasCause<SocketTimeoutException>()) {
                    BillLogErrorCode.RECEIPT_ANALYSIS_TIMEOUT
                } else {
                    BillLogErrorCode.RECEIPT_ANALYSIS_FAILED
                }
            throw BusinessException(errorCode, exception)
        }
    }

    private inline fun <reified T : Throwable> Throwable.hasCause(): Boolean =
        generateSequence(this) { throwable -> throwable.cause }.any { throwable -> throwable is T }

    private data class UpstageExtractionRequest(
        val model: String,
        val messages: List<UpstageMessage>,
        @field:JsonProperty("response_format")
        val responseFormat: Map<String, Any>,
        val mode: String,
    )

    private data class UpstageMessage(
        val role: String,
        val content: List<UpstageImageContent>,
    )

    private data class UpstageImageContent(
        val type: String,
        @field:JsonProperty("image_url")
        val imageUrl: UpstageImageUrl,
    )

    private data class UpstageImageUrl(
        val url: String,
    )

    private data class UpstageExtractionResponse(
        val choices: List<UpstageChoice> = emptyList(),
    )

    private data class UpstageChoice(
        val message: UpstageResponseMessage,
    )

    private data class UpstageResponseMessage(
        val content: String?,
    )

    companion object {
        private const val CHAT_COMPLETIONS_PATH = "chat/completions"
        private const val USER_ROLE = "user"
        private const val IMAGE_URL_TYPE = "image_url"
        private val log = LoggerFactory.getLogger(UpstageReceiptExtractionClient::class.java)

        private val RECEIPT_RESPONSE_FORMAT: Map<String, Any> =
            mapOf(
                "type" to "json_schema",
                "json_schema" to
                    mapOf(
                        "name" to "receipt",
                        "schema" to receiptSchema(),
                    ),
            )

        private fun receiptSchema(): Map<String, Any> =
            mapOf(
                "type" to "object",
                "properties" to
                    mapOf(
                        "title" to stringProperty("영수증의 상호명 또는 결제 제목. 확인할 수 없으면 생략"),
                        "paymentDate" to stringProperty("결제일. 확인 가능한 경우 YYYY-MM-DD 형식으로 기재하고 아니면 생략"),
                        "items" to
                            mapOf(
                                "type" to "array",
                                "description" to "영수증에 표시된 결제 항목 목록",
                                "items" to
                                    mapOf(
                                        "type" to "object",
                                        "properties" to
                                            mapOf(
                                                "name" to stringProperty("결제 항목명. 확인할 수 없으면 생략"),
                                                "amount" to numberProperty("해당 항목의 최종 결제 금액. 확인할 수 없으면 생략"),
                                            ),
                                        "additionalProperties" to false,
                                    ),
                            ),
                        "totalAmount" to numberProperty("영수증에 표시된 최종 결제 총액. 확인할 수 없으면 생략"),
                    ),
                "required" to listOf("items"),
                "additionalProperties" to false,
            )

        private fun stringProperty(description: String): Map<String, Any> =
            mapOf(
                "type" to "string",
                "description" to description,
            )

        private fun numberProperty(description: String): Map<String, Any> =
            mapOf(
                "type" to "number",
                "description" to description,
            )
    }
}
