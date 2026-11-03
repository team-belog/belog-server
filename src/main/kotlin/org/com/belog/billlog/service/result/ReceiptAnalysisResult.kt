package org.com.belog.billlog.service.result

import java.math.BigDecimal
import java.time.LocalDate

data class ReceiptAnalysisResult(
    val title: String?,
    val paymentDate: LocalDate?,
    val items: List<ReceiptAnalysisItemResult>,
    val totalAmount: BigDecimal?,
    val requiresConfirmation: Boolean,
)

data class ReceiptAnalysisItemResult(
    val name: String?,
    val amount: BigDecimal?,
)
