package org.com.belog.global.storage

import org.com.belog.global.config.S3StorageProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import software.amazon.awssdk.core.exception.SdkException
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception

@Component
class S3ObjectMetadataProvider(
    private val s3Client: S3Client,
    private val properties: S3StorageProperties,
) {
    fun get(objectKey: String): S3ObjectMetadata {
        val response =
            try {
                s3Client.headObject(
                    HeadObjectRequest
                        .builder()
                        .bucket(properties.bucket)
                        .key(objectKey)
                        .build(),
                )
            } catch (exception: S3Exception) {
                if (exception.statusCode() == NOT_FOUND_STATUS_CODE) {
                    throw S3ObjectNotFoundException(objectKey, exception)
                }
                log.error(
                    "S3 object metadata request failed: status={}, errorCode={}, requestId={}",
                    exception.statusCode(),
                    exception.awsErrorDetails()?.errorCode(),
                    exception.requestId(),
                    exception,
                )
                throw S3ObjectUnavailableException(objectKey, exception)
            } catch (exception: SdkException) {
                log.error("S3 object metadata request could not be completed", exception)
                throw S3ObjectUnavailableException(objectKey, exception)
            }

        return S3ObjectMetadata(
            contentType = response.contentType(),
            contentLength = response.contentLength(),
        )
    }

    companion object {
        private const val NOT_FOUND_STATUS_CODE = 404
        private val log = LoggerFactory.getLogger(S3ObjectMetadataProvider::class.java)
    }
}
