package org.com.belog.prelog.controller.cursor

import org.com.belog.prelog.service.query.PlanListCursor
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object PlanListCursorCodec {
    private const val PINNED_VALUE = "1"
    private const val UNPINNED_VALUE = "0"
    private const val SEPARATOR = ":"
    private const val CURSOR_PART_COUNT = 2

    fun encode(cursor: PlanListCursor): String {
        val pinnedValue = if (cursor.pinned) PINNED_VALUE else UNPINNED_VALUE
        val rawCursor = "$pinnedValue$SEPARATOR${cursor.planId}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(value: String?): PlanListCursor? {
        if (value == null) {
            return null
        }

        try {
            val decodedValue = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
            val parts = decodedValue.split(SEPARATOR, limit = CURSOR_PART_COUNT)
            require(parts.size == CURSOR_PART_COUNT)

            val pinned =
                when (parts[0]) {
                    PINNED_VALUE -> true
                    UNPINNED_VALUE -> false
                    else -> throw IllegalArgumentException()
                }

            return PlanListCursor(pinned = pinned, planId = parts[1].toLong())
        } catch (exception: RuntimeException) {
            throw IllegalArgumentException("계획 목록 커서 형식이 올바르지 않습니다.", exception)
        }
    }
}
