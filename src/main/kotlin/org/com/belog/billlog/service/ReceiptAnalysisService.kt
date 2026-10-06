package org.com.belog.billlog.service

import org.com.belog.billlog.domain.ReceiptExtraction
import org.com.belog.billlog.domain.ReceiptExtractionItem
import org.com.belog.billlog.infrastructure.UpstageReceiptExtractionClient
import org.com.belog.billlog.service.result.ReceiptAnalysisItemResult
import org.com.belog.billlog.service.result.ReceiptAnalysisResult
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.BigInteger
import java.time.LocalDate

@Service
class ReceiptAnalysisService(
    private val receiptImageService: ReceiptImageService,
    private val receiptExtractionClient: UpstageReceiptExtractionClient,
) {
    fun analyze(
        meetingId: Long,
        userId: Long,
        objectKey: String,
    ): ReceiptAnalysisResult {
        val source =
            receiptImageService.prepareAnalysisSource(
                meetingId = meetingId,
                userId = userId,
                objectKeyValue = objectKey,
            )
        val extraction = receiptExtractionClient.extract(source.readUrl)

        return extraction.toAnalysisResult()
    }

    private fun ReceiptExtraction.toAnalysisResult(): ReceiptAnalysisResult {
        val normalizedItems = items.map { item -> item.toAnalysisItemResult() }
        val normalizedTotalAmount = totalAmount.toLongExactOrNull()

        return ReceiptAnalysisResult(
            title = title.normalizeText(),
            paymentDate = paymentDate.toLocalDateOrNull(),
            items = normalizedItems,
            totalAmount = normalizedTotalAmount,
            requiresConfirmation = requiresAmountConfirmation(normalizedItems, normalizedTotalAmount),
        )
    }

    private fun ReceiptExtractionItem.toAnalysisItemResult(): ReceiptAnalysisItemResult =
        ReceiptAnalysisItemResult(
            name = name.normalizeText(),
            amount = amount.toLongExactOrNull(),
        )

    private fun requiresAmountConfirmation(
        items: List<ReceiptAnalysisItemResult>,
        totalAmount: Long?,
    ): Boolean {
        if (items.isEmpty() || totalAmount == null || items.any { item -> item.amount == null }) {
            return true
        }

        val itemTotal =
            items.fold(BigInteger.ZERO) { sum, item ->
                sum + BigInteger.valueOf(checkNotNull(item.amount))
            }
        return itemTotal != BigInteger.valueOf(totalAmount)
    }

    private fun String?.normalizeText(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private fun String?.toLocalDateOrNull(): LocalDate? =
        normalizeText()?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

    private fun BigDecimal?.toLongExactOrNull(): Long? = this?.let { value -> runCatching(value::longValueExact).getOrNull() }
}
