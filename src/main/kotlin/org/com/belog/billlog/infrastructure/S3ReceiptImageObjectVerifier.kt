package org.com.belog.billlog.infrastructure

import org.com.belog.billlog.code.BillLogErrorCode
import org.com.belog.billlog.domain.ReceiptImageFormat
import org.com.belog.billlog.domain.ReceiptImageObjectKey
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.springframework.stereotype.Component

@Component
class S3ReceiptImageObjectVerifier(
    private val s3ObjectMetadataProvider: S3ObjectMetadataProvider,
) {
    fun verify(objectKey: ReceiptImageObjectKey) {
        val metadata =
            try {
                s3ObjectMetadataProvider.get(objectKey.value)
            } catch (exception: S3ObjectNotFoundException) {
                throw BusinessException(BillLogErrorCode.RECEIPT_IMAGE_NOT_FOUND, exception)
            }

        val format =
            metadata.contentType?.let(ReceiptImageFormat::fromContentType)
                ?: throw BusinessException(BillLogErrorCode.INVALID_RECEIPT_IMAGE_METADATA)
        val hasValidSize = metadata.contentLength in 1L..ReceiptImageFormat.MAX_FILE_SIZE_BYTES
        val hasMatchingFormat = objectKey.format == format

        if (!hasValidSize || !hasMatchingFormat) {
            throw BusinessException(BillLogErrorCode.INVALID_RECEIPT_IMAGE_METADATA)
        }
    }
}
