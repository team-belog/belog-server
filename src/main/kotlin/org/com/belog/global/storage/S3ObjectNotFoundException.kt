package org.com.belog.global.storage

class S3ObjectNotFoundException(
    val objectKey: String,
    cause: Throwable,
) : RuntimeException(cause)
