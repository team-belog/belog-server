package org.com.belog.user.infrastructure

import org.com.belog.global.storage.S3ObjectReadUrlProvider
import org.com.belog.user.domain.ProfileImageFormat
import org.com.belog.user.domain.ProfileImageObjectKey
import org.com.belog.user.domain.ProfileImageUpload
import org.springframework.stereotype.Component

@Component
class ProfileImageStorage(
    private val uploadUrlProvider: S3ProfileImageUploadUrlProvider,
    private val objectReadUrlProvider: S3ObjectReadUrlProvider,
    private val objectVerifier: S3ProfileImageObjectVerifier,
) {
    fun issueUploadUrl(
        userId: Long,
        format: ProfileImageFormat,
        fileSize: Long,
    ): ProfileImageUpload = uploadUrlProvider.issueUploadUrl(userId, format, fileSize)

    fun verify(objectKey: ProfileImageObjectKey) {
        objectVerifier.verify(objectKey)
    }

    fun generateReadUrl(objectKey: String): String = objectReadUrlProvider.generateReadUrl(objectKey)
}
