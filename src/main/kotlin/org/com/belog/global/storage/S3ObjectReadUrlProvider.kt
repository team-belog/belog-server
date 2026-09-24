package org.com.belog.global.storage

import org.com.belog.global.config.S3StorageProperties
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest

@Component
class S3ObjectReadUrlProvider(
    private val s3Presigner: S3Presigner,
    private val properties: S3StorageProperties,
) {
    fun generateReadUrl(objectKey: String): String {
        val getObjectRequest =
            GetObjectRequest
                .builder()
                .bucket(properties.bucket)
                .key(objectKey)
                .build()
        val presignedRequest =
            s3Presigner.presignGetObject(
                GetObjectPresignRequest
                    .builder()
                    .signatureDuration(properties.presignExpiration)
                    .getObjectRequest(getObjectRequest)
                    .build(),
            )

        return presignedRequest.url().toExternalForm()
    }
}
