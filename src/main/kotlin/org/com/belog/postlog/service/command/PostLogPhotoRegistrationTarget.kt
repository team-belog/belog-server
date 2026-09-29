package org.com.belog.postlog.service.command

import java.time.OffsetDateTime

data class PostLogPhotoRegistrationTarget(
    val objectKey: String,
    val capturedAt: OffsetDateTime,
)
