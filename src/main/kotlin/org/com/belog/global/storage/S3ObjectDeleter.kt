package org.com.belog.global.storage

import org.com.belog.global.config.S3StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Component
class S3ObjectDeleter(
    private val s3Client: S3Client,
    private val properties: S3StorageProperties,
) {
    fun delete(objectKey: String) {
        try {
            s3Client.deleteObject(
                DeleteObjectRequest
                    .builder()
                    .bucket(properties.bucket)
                    .key(objectKey)
                    .build(),
            )
        } catch (exception: S3Exception) {
            log.error(
                "S3 object delete request failed: status={}, errorCode={}, requestId={}",
                exception.statusCode(),
                exception.awsErrorDetails()?.errorCode(),
                exception.requestId(),
                exception,
            )
            throw S3ObjectUnavailableException(objectKey, exception)
        } catch (exception: SdkException) {
            log.error("S3 object delete request could not be completed", exception)
            throw S3ObjectUnavailableException(objectKey, exception)
        }
    }

    companion object {
        private val log = LoggerFactory.getLogger(S3ObjectDeleter::class.java)
    }
}
