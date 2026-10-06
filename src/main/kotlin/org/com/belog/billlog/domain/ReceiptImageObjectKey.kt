package org.com.belog.billlog.domain

import java.util.UUID

@JvmInline
value class ReceiptImageObjectKey private constructor(
    val value: String,
) {
    val format: ReceiptImageFormat
        get() = checkNotNull(ReceiptImageFormat.fromExtension(value.substringAfterLast('.')))

    companion object {
        const val MAX_LENGTH = 1024

        fun create(
            meetingId: Long,
            userId: Long,
            value: String,
        ): ReceiptImageObjectKey {
            require(meetingId > 0) { "만남 식별자는 양수여야 합니다." }
            require(userId > 0) { "사용자 식별자는 양수여야 합니다." }
            require(value.isNotBlank()) { "영수증 이미지 object key는 비어 있을 수 없습니다." }
            require(value.length <= MAX_LENGTH) { "영수증 이미지 object key는 ${MAX_LENGTH}자를 초과할 수 없습니다." }
            require(value.none(Char::isWhitespace)) { "영수증 이미지 object key에는 공백을 포함할 수 없습니다." }

            val prefix = "bill-log/receipts/$meetingId/$userId/"
            require(value.startsWith(prefix)) { "현재 사용자가 업로드한 영수증 이미지 경로가 아닙니다." }

            val fileName = value.removePrefix(prefix)
            require(fileName.isNotEmpty() && fileName.none { character -> character == '/' || character == '\\' }) {
                "영수증 이미지 파일명이 올바르지 않습니다."
            }

            val extensionSeparatorIndex = fileName.lastIndexOf('.')
            require(extensionSeparatorIndex > 0 && extensionSeparatorIndex < fileName.lastIndex) {
                "영수증 이미지 확장자가 필요합니다."
            }

            val imageIdValue = fileName.substring(0, extensionSeparatorIndex)
            val extension = fileName.substring(extensionSeparatorIndex + 1)
            require(ReceiptImageFormat.fromExtension(extension) != null) { "지원하지 않는 영수증 이미지 확장자입니다." }

            val imageId = runCatching { UUID.fromString(imageIdValue) }.getOrNull()
            require(imageId != null && imageId.toString().equals(imageIdValue, ignoreCase = true)) {
                "영수증 이미지 파일 식별자가 올바르지 않습니다."
            }

            return ReceiptImageObjectKey(value)
        }
    }
}
