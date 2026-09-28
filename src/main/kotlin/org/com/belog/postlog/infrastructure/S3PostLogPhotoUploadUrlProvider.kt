package org.com.belog.postlog.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.postlog.domain.PostLogPhotoFormat
import org.com.belog.postlog.domain.PostLogPhotoUpload
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.util.UUID

@Component
class S3PostLogPhotoUploadUrlProvider(
    private val s3Presigner: S3Presigner,
    private val properties: S3StorageProperties,
) {
    fun issueUploadUrl(
        meetingId: Long,
        format: PostLogPhotoFormat,
        fileSize: Long,
    ): PostLogPhotoUpload {
        val objectKey = createObjectKey(meetingId, format.extension)
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .contentType(format.contentType)
                .contentLength(fileSize)
                .build()
        val presignedRequest =
            s3Presigner.presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(properties.presignExpiration)
                    .putObjectRequest(putObjectRequest)
                    .build(),
            )

        return PostLogPhotoUpload(
            objectKey = objectKey,
            uploadUrl = presignedRequest.url().toExternalForm(),
            contentType = format.contentType,
            contentLength = fileSize,
            expiresAt = presignedRequest.expiration(),
        )
    }

    private fun createObjectKey(
        meetingId: Long,
        extension: String,
    ): String = "post-logs/$meetingId/photos/${UUID.randomUUID()}.$extension"
}
