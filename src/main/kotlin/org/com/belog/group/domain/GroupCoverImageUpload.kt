package org.com.belog.group.domain

import java.time.Instant

data class GroupCoverImageUpload(
    val objectKey: String,
    val uploadUrl: String,
    val contentType: String,
    val contentLength: Long,
    val expiresAt: Instant,
)
