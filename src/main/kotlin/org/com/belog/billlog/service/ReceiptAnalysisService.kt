package org.com.belog.billlog.service

import org.com.belog.billlog.domain.BillAmount
import org.com.belog.billlog.domain.ReceiptExtraction
import org.com.belog.billlog.domain.ReceiptExtractionItem
import org.com.belog.billlog.infrastructure.UpstageReceiptExtractionClient
import org.com.belog.billlog.service.result.ReceiptAnalysisItemResult
import org.com.belog.billlog.service.result.ReceiptAnalysisResult
import org.springframework.stereotype.Service
import java.math.BigDecimal
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
        val normalizedTotalAmount = totalAmount.toBillAmountOrNull()

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
            amount = amount.toBillAmountOrNull(),
        )

    private fun requiresAmountConfirmation(
        items: List<ReceiptAnalysisItemResult>,
        totalAmount: BigDecimal?,
    ): Boolean {
        if (items.isEmpty() || totalAmount == null || items.any { item -> item.amount == null }) {
            return true
        }

        val itemTotal =
            items.fold(BigDecimal.ZERO) { sum, item ->
                sum.add(checkNotNull(item.amount))
            }
        return itemTotal.compareTo(totalAmount) != 0
    }

    private fun String?.normalizeText(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    private fun String?.toLocalDateOrNull(): LocalDate? =
        normalizeText()?.let { value -> runCatching { LocalDate.parse(value) }.getOrNull() }

    private fun BigDecimal?.toBillAmountOrNull(): BigDecimal? {
        val strippedAmount = this?.stripTrailingZeros() ?: return null
        val amount = if (strippedAmount.scale() < 0) strippedAmount.setScale(0) else strippedAmount
        return amount.takeIf { value -> value.scale() <= BillAmount.SCALE && BillAmount.isStorable(value) }
    }
}
