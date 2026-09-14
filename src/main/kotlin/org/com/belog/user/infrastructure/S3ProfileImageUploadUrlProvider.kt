package org.com.belog.user.infrastructure

import org.com.belog.global.config.S3StorageProperties
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageUpload
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.util.UUID

@Component
class S3ProfileImageUploadUrlProvider(
    private val s3Presigner: S3Presigner,
    private val properties: S3StorageProperties,
) {
    fun issueUploadUrl(
        userId: Long,
        format: ProfileImageFormat,
    ): ProfileImageUpload {
        val objectKey = createObjectKey(userId, format.extension)
        val putObjectRequest =
            PutObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .contentType(format.contentType)
                .build()
        val presignedRequest =
            s3Presigner.presignPutObject(
                PutObjectPresignRequest
                    .builder()
                    .signatureDuration(properties.presignExpiration)
                    .putObjectRequest(putObjectRequest)
                    .build(),
            )

        return ProfileImageUpload(
            objectKey = objectKey,
            uploadUrl = presignedRequest.url().toExternalForm(),
            contentType = format.contentType,
            expiresAt = presignedRequest.expiration(),
        )
    }

    private fun createObjectKey(
        userId: Long,
        extension: String,
    ): String = "users/$userId/profile/${UUID.randomUUID()}.$extension"
}
