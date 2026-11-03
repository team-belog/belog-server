package org.com.belog.billlog.domain

import java.util.Locale

enum class ReceiptImageFormat(
    val contentType: String,
    val extension: String,
) {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    ;

    companion object {
        const val MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024

        fun fromContentType(contentType: String): ReceiptImageFormat? {
            val normalizedContentType = contentType.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { format -> format.contentType == normalizedContentType }
        }

        fun fromExtension(extension: String): ReceiptImageFormat? {
            val normalizedExtension = extension.trim().lowercase(Locale.ROOT)
            return entries.firstOrNull { format -> format.extension == normalizedExtension }
        }
    }
}
