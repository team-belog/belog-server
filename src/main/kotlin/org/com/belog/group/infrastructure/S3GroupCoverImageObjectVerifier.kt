package org.com.belog.group.infrastructure

import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.springframework.stereotype.Component

@Component
class S3GroupCoverImageObjectVerifier(
    private val s3ObjectMetadataProvider: S3ObjectMetadataProvider,
) {
    fun verify(objectKey: GroupCoverImageObjectKey) {
        val metadata =
            try {
                s3ObjectMetadataProvider.get(objectKey.value)
            } catch (exception: S3ObjectNotFoundException) {
                throw BusinessException(GroupErrorCode.COVER_IMAGE_NOT_FOUND, exception)
            }

        val format =
            metadata.contentType?.let(GroupCoverImageFormat::fromContentType)
                ?: throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_METADATA)
        val hasValidSize = metadata.contentLength in 1L..GroupCoverImageFormat.MAX_FILE_SIZE_BYTES
        val hasMatchingExtension = objectKey.value.endsWith(".${format.extension}")

        if (!hasValidSize || !hasMatchingExtension) {
            throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_METADATA)
        }
    }
}
