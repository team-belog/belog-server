package org.com.belog.postlog.domain

import java.time.Instant

data class PostLogPhotoUpload(
    val objectKey: String,
    val uploadUrl: String,
    val contentType: String,
    val contentLength: Long,
    val expiresAt: Instant,
)
