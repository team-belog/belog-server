package org.com.belog.billlog.service

import org.com.belog.billlog.domain.ReceiptExtraction
import org.com.belog.billlog.domain.ReceiptExtractionItem
import org.com.belog.billlog.domain.ReceiptImageObjectKey
import org.com.belog.billlog.domain.ReceiptImageSource
import org.com.belog.billlog.infrastructure.UpstageReceiptExtractionClient
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import java.math.BigDecimal
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReceiptAnalysisServiceTest {
    private val imageService = mock(ReceiptImageService::class.java)
    private val extractionClient = mock(UpstageReceiptExtractionClient::class.java)
    private val service = ReceiptAnalysisService(imageService, extractionClient)
    private val objectKey = "bill-log/receipts/7/15/550e8400-e29b-41d4-a716-446655440000.jpg"
    private val readUrl = "https://receipt.example/image.jpg"

    @Test
    fun `항목 합계와 총액이 같으면 확인이 필요하지 않다`() {
        val result = analyze(listOf(1000L, 2000L), 3000L)

        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun `항목 합계와 총액이 다르면 확인이 필요하다`() {
        val result = analyze(listOf(1000L, 2000L), 4000L)

        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun `항목 금액이 누락되면 확인이 필요하다`() {
        val result = analyze(listOf(1000L, null), 1000L)

        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun `저장할 수 없는 큰 금액은 비우고 확인이 필요하다`() {
        val result = analyze(listOf(Long.MAX_VALUE), Long.MAX_VALUE)

        assertEquals(listOf(null), result.items.map { item -> item.amount })
        assertNull(result.totalAmount)
        assertTrue(result.requiresConfirmation)
    }

    @Test
    fun `소수 금액을 그대로 반환하고 합계를 비교한다`() {
        givenExtraction(listOf(BigDecimal("4.5"), BigDecimal("8.00")), BigDecimal("12.50"))

        val result = service.analyze(7L, 15L, objectKey)

        assertEquals(listOf(BigDecimal("4.5"), BigDecimal("8")), result.items.map { item -> item.amount })
        assertEquals(BigDecimal("12.5"), result.totalAmount)
        assertFalse(result.requiresConfirmation)
    }

    @Test
    fun `이미지 접근 권한이 없으면 Upstage를 호출하지 않는다`() {
        `when`(imageService.prepareAnalysisSource(7L, 15L, objectKey)).thenThrow(BusinessException(GroupErrorCode.NOT_GROUP_MEMBER))

        assertFailsWith<BusinessException> { service.analyze(7L, 15L, objectKey) }

        verifyNoInteractions(extractionClient)
    }

    private fun analyze(
        amounts: List<Long?>,
        totalAmount: Long,
    ): org.com.belog.billlog.service.result.ReceiptAnalysisResult {
        givenExtraction(amounts.map { amount -> amount?.let(BigDecimal::valueOf) }, BigDecimal.valueOf(totalAmount))
        return service.analyze(7L, 15L, objectKey)
    }

    private fun givenExtraction(
        amounts: List<BigDecimal?>,
        totalAmount: BigDecimal,
    ) {
        val source = ReceiptImageSource(ReceiptImageObjectKey.create(7L, 15L, objectKey), readUrl)
        `when`(imageService.prepareAnalysisSource(7L, 15L, objectKey)).thenReturn(source)
        `when`(extractionClient.extract(readUrl)).thenReturn(
            ReceiptExtraction(
                items = amounts.map { amount -> ReceiptExtractionItem(amount = amount) },
                totalAmount = totalAmount,
            ),
        )
    }
}
