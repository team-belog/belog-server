package org.com.belog.postlog.infrastructure

import jakarta.annotation.PreDestroy
import org.com.belog.global.error.BusinessException
import org.com.belog.global.storage.S3ObjectMetadataProvider
import org.com.belog.global.storage.S3ObjectNotFoundException
import org.com.belog.postlog.code.PostLogErrorCode
import org.com.belog.postlog.domain.PostLogPhoto
import org.com.belog.postlog.domain.PostLogPhotoFormat
import org.com.belog.postlog.domain.PostLogPhotoObjectKey
import org.springframework.stereotype.Component
import java.util.concurrent.ExecutionException
import java.util.concurrent.ExecutorCompletionService
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

@Component
class S3PostLogPhotoObjectVerifier(
    private val s3ObjectMetadataProvider: S3ObjectMetadataProvider,
) {
    private val executor: ExecutorService = Executors.newFixedThreadPool(MAX_CONCURRENT_HEAD_REQUESTS)

    fun verifyAll(objectKeys: List<PostLogPhotoObjectKey>) {
        if (
            objectKeys.size !in 1..PostLogPhoto.MAX_UPLOAD_COUNT ||
            objectKeys.distinct().size != objectKeys.size
        ) {
            throw BusinessException(PostLogErrorCode.INVALID_PHOTO_UPLOAD_REQUEST)
        }

        val completionService = ExecutorCompletionService<Unit>(executor)
        val futures = objectKeys.map { objectKey -> completionService.submit { verify(objectKey) } }

        try {
            repeat(futures.size) {
                completionService.take().get()
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("Post-log 사진 객체 검증이 중단되었습니다.", exception)
        } catch (exception: ExecutionException) {
            val cause = exception.cause
            if (cause is RuntimeException) {
                throw cause
            }
            throw IllegalStateException("Post-log 사진 객체를 검증할 수 없습니다.", cause)
        } finally {
            futures.forEach(::cancel)
        }
    }

    private fun verify(objectKey: PostLogPhotoObjectKey) {
        val metadata =
            try {
                s3ObjectMetadataProvider.get(objectKey.value)
            } catch (exception: S3ObjectNotFoundException) {
                throw BusinessException(PostLogErrorCode.PHOTO_NOT_FOUND, exception)
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
        executor.shutdown()
    }

    companion object {
        private const val MAX_CONCURRENT_HEAD_REQUESTS = 10
    }
}
