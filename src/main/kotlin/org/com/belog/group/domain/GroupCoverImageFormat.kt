package org.com.belog.group.domain

import java.util.Locale

enum class GroupCoverImageFormat(
    val contentType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp"),
    ;

    companion object {
        const val MAX_FILE_SIZE_BYTES = 5L * 1024 * 1024

        fun fromContentType(contentType: String): GroupCoverImageFormat? {
            val normalizedContentType = contentType.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { format -> format.contentType == normalizedContentType }
        }
    }
}
