package org.com.belog.home.controller.cursor

import org.com.belog.global.error.BusinessException
import org.com.belog.home.code.HomeErrorCode
import java.time.format.DateTimeParseException

internal inline fun <T> decodeHomeCursor(block: () -> T): T =
    try {
        block()
    } catch (exception: IllegalArgumentException) {
        throw BusinessException(HomeErrorCode.INVALID_CURSOR, exception)
    } catch (exception: DateTimeParseException) {
        throw BusinessException(HomeErrorCode.INVALID_CURSOR, exception)
    }
