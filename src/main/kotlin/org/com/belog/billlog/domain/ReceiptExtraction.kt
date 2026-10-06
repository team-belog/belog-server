package org.com.belog.billlog.domain

import java.math.BigDecimal

data class ReceiptExtraction(
    val title: String?,
    val paymentDate: String?,
    val items: List<ReceiptExtractionItem>,
    val totalAmount: BigDecimal?,
)

data class ReceiptExtractionItem(
    val name: String?,
    val amount: BigDecimal?,
)
