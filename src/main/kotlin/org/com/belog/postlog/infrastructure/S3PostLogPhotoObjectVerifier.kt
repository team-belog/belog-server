package org.com.belog.postlog.infrastructure

import jakarta.annotation.PreDestroy
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.com.belog.global.storage.S3ObjectUnavailableException
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.config.PostLogPhotoVerificationProperties
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoFormat
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.springframework.stereotype.Component
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.Future
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

@Component
class S3PostLogPhotoObjectVerifier(
    private val s3ObjectMetadataProvider: S3ObjectMetadataProvider,
    private val properties: PostLogPhotoVerificationProperties,
) {
    private val executor =
        ThreadPoolExecutor(
            properties.maxConcurrentHeadRequests,
            properties.maxConcurrentHeadRequests,
            0L,
            TimeUnit.MILLISECONDS,
            ArrayBlockingQueue(properties.queueCapacity),
            Thread
                .ofPlatform()
                .name("post-log-photo-verifier-", 0)
                .daemon(true)
                .factory(),
            ThreadPoolExecutor.AbortPolicy(),
        )

    fun verifyAll(objectKeys: List<PostLogPhotoObjectKey>) {
        if (
            objectKeys.size !in 1..PostLogPhoto.MAX_UPLOAD_COUNT ||
            objectKeys.distinct().size != objectKeys.size
        ) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }

        val completionService = ExecutorCompletionService<Unit>(executor)
        val futures = mutableListOf<Future<Unit>>()
        val deadlineNanos = System.nanoTime() + properties.timeout.toNanos()

        try {
            objectKeys.forEach { objectKey ->
                futures += completionService.submit { verify(objectKey) }
            }
            repeat(futures.size) {
                val remainingNanos = deadlineNanos - System.nanoTime()
                if (remainingNanos <= 0L) {
                    throw verificationUnavailable()
                }
                val completedFuture =
                    completionService.poll(remainingNanos, TimeUnit.NANOSECONDS)
                        ?: throw verificationUnavailable()
                completedFuture.get()
            }
        } catch (exception: RejectedExecutionException) {
            throw verificationUnavailable(exception)
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw verificationUnavailable(exception)
        } catch (exception: ExecutionException) {
            val cause = exception.cause
            if (cause is RuntimeException) {
                throw cause
            }
            throw IllegalStateException("Post-log 사진 객체를 검증할 수 없습니다.", cause)
        } finally {
            futures.forEach(::cancel)
            executor.purge()
        }
    }

    private fun verify(objectKey: PostLogPhotoObjectKey) {
        val metadata =
            try {
                s3ObjectMetadataProvider.get(objectKey.value)
            } catch (exception: S3ObjectNotFoundException) {
                throw BusinessException(PostLogErrorCode.PHOTO_NOT_FOUND, exception)
            } catch (exception: S3ObjectUnavailableException) {
                throw BusinessException(PostLogErrorCode.PHOTO_VERIFICATION_UNAVAILABLE, exception)
            }

        val format =
            metadata.contentType?.let(PostLogPhotoFormat::fromContentType)
                ?: throw BusinessException(PostLogErrorCode.INVALID_PHOTO_METADATA)
        val hasValidSize = metadata.contentLength in 1L..PostLogPhotoFormat.MAX_FILE_SIZE_BYTES
        val hasMatchingExtension = objectKey.value.endsWith(".${format.extension}")

        if (!hasValidSize || !hasMatchingExtension) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_METADATA)
        }
    }

    private fun cancel(future: Future<Unit>) {
        if (!future.isDone) {
            future.cancel(true)
        }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdownNow()
    }

    private fun verificationUnavailable(cause: Throwable? = null): BusinessException =
        BusinessException(PostLogErrorCode.PHOTO_VERIFICATION_UNAVAILABLE, cause)
}
