package org.com.belog.billlog.domain

import java.time.Instant

data class ReceiptImageUpload(
    val objectKey: String,
    val uploadUrl: String,
    val contentType: String,
    val contentLength: Long,
    val expiresAt: Instant,
)
