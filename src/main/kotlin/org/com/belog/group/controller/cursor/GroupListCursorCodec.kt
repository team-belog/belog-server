package org.com.belog.group.controller.cursor

import org.com.belog.group.service.query.GroupListCursor
import java.nio.charset.StandardCharsets
import java.util.Base64

internal object GroupListCursorCodec {
    private const val PINNED_VALUE = "1"
    private const val UNPINNED_VALUE = "0"
    private const val SEPARATOR = ":"

    fun encode(cursor: GroupListCursor): String {
        val pinnedValue = if (cursor.pinned) PINNED_VALUE else UNPINNED_VALUE
        val rawCursor = "$pinnedValue$SEPARATOR${cursor.groupMemberId}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(value: String?): GroupListCursor? {
        if (value == null) {
            return null
        }

        val decodedValue = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        val parts = decodedValue.split(SEPARATOR)
        require(parts.size == 2) { "그룹 목록 커서 형식이 올바르지 않습니다." }

        val pinned =
            when (parts[0]) {
                PINNED_VALUE -> true
                UNPINNED_VALUE -> false
                else -> throw IllegalArgumentException("그룹 목록 커서 형식이 올바르지 않습니다.")
            }

        return GroupListCursor(
            pinned = pinned,
            groupMemberId = parts[1].toLong(),
        )
    }
}
