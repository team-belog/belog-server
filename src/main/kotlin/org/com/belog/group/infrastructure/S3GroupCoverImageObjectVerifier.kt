package org.com.belog.group.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.global.error.BusinessException
import org.com.belog.group.code.GroupErrorCode
import org.com.belog.group.domain.GroupCoverImageFormat
import org.com.belog.group.domain.GroupCoverImageObjectKey
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Component
class S3GroupCoverImageObjectVerifier(
    private val s3Client: S3Client,
    private val properties: S3StorageProperties,
) {
    fun verify(objectKey: GroupCoverImageObjectKey) {
        val response =
            try {
                s3Client.headObject(
                    HeadObjectRequest
                        .builder()
                        .bucket(properties.bucket)
                        .key(objectKey.value)
                        .build(),
                )
            } catch (exception: S3Exception) {
                if (exception.statusCode() == NOT_FOUND_STATUS_CODE) {
                    throw BusinessException(GroupErrorCode.COVER_IMAGE_NOT_FOUND, exception)
                }
                throw exception
            }

        val format =
            response.contentType()?.let(GroupCoverImageFormat::fromContentType)
                ?: throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_METADATA)
        val hasValidSize = response.contentLength() in 1L..GroupCoverImageFormat.MAX_FILE_SIZE_BYTES
        val hasMatchingExtension = objectKey.value.endsWith(".${format.extension}")

        if (!hasValidSize || !hasMatchingExtension) {
            throw BusinessException(GroupErrorCode.INVALID_COVER_IMAGE_METADATA)
        }
    }

    companion object {
        private const val NOT_FOUND_STATUS_CODE = 404
    }
}
