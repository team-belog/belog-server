package org.com.belog.postlog.service.result

import org.com.belog.postlog.domain.PostLogPhotoUpload

data class PostLogPhotoUploadResult(
    val clientPhotoId: String,
    val upload: PostLogPhotoUpload,
)
