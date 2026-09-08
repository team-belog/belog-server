package org.com.belog.global.error

import org.com.belog.global.response.code.ErrorCode

class BusinessException(
    val errorCode: ErrorCode,
    cause: Throwable? = null,
) : RuntimeException(errorCode.message, cause)
