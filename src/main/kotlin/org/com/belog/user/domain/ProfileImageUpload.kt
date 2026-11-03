package org.com.belog.user.domain

import java.time.Instant

data class ProfileImageUpload(
    val objectKey: String,
    val uploadUrl: String,
    val contentType: String,
    val contentLength: Long,
    val expiresAt: Instant,
)
