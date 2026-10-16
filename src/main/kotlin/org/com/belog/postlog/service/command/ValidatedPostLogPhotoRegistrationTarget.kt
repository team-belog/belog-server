package org.com.belog.postlog.service.command

import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import java.time.OffsetDateTime

data class ValidatedPostLogPhotoRegistrationTarget(
    val objectKey: PostLogPhotoObjectKey,
    val capturedAt: OffsetDateTime,
)
