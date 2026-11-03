package org.com.belog.billlog.domain

import java.math.BigDecimal

data class ReceiptExtraction(
    val title: String? = null,
    val paymentDate: String? = null,
    val items: List<ReceiptExtractionItem> = emptyList(),
    val totalAmount: BigDecimal? = null,
)

data class ReceiptExtractionItem(
    val name: String? = null,
    val amount: BigDecimal? = null,
)
