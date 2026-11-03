package org.com.belog.global.storage

class S3ObjectUnavailableException(
    val objectKey: String,
    cause: Throwable,
) : RuntimeException(cause)
