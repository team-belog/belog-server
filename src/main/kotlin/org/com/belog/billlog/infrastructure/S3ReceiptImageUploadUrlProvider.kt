package org.com.belog.billlog.infrastructure

import org.com.belog.billlog.domain.ReceiptImageFormat
import org.com.belog.billlog.domain.ReceiptImageUpload
import org.com.belog.global.config.S3StorageProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest
import java.util.UUID

@Component
class S3ReceiptImageUploadUrlProvider(
    private val s3Presigner: S3Presigner,
    private val properties: S3StorageProperties,
) {
    fun issueUploadUrl(
        meetingId: Long,
        userId: Long,
        format: ReceiptImageFormat,
        fileSize: Long,
    ): ReceiptImageUpload {
        val objectKey = createObjectKey(meetingId, userId, format.extension)
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

        return ReceiptImageUpload(
            objectKey = objectKey,
            uploadUrl = presignedRequest.url().toExternalForm(),
            contentType = format.contentType,
            contentLength = fileSize,
            expiresAt = presignedRequest.expiration(),
        )
    }

    private fun createObjectKey(
        meetingId: Long,
        userId: Long,
        extension: String,
    ): String = "bill-log/receipts/$meetingId/$userId/${UUID.randomUUID()}.$extension"
}
