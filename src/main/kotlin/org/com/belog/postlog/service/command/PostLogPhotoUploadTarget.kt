package org.com.belog.postlog.service.command

data class PostLogPhotoUploadTarget(
    val clientPhotoId: String,
    val contentType: String,
    val fileSize: Long,
) {
    companion object {
        const val CLIENT_PHOTO_ID_MAX_LENGTH = 100
    }
}
