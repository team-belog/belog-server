package org.com.belog.home.controller.cursor

import org.com.belog.home.service.query.ActiveMeetingCursor
import org.com.belog.home.service.query.ActiveMeetingCursorType
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Base64

internal object ActiveMeetingCursorCodec {
    private const val SEPARATOR = ":"
    private const val EMPTY_DATE = "_"

    fun encode(cursor: ActiveMeetingCursor): String {
        val rawCursor =
            "${cursor.type}$SEPARATOR${cursor.startDate ?: EMPTY_DATE}$SEPARATOR${cursor.meetingId}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(value: String?): ActiveMeetingCursor? {
        if (value == null) {
            return null
        }

        val decodedValue = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
        val parts = decodedValue.split(SEPARATOR)
        require(parts.size == 3) { "진행 중인 만남 목록 커서 형식이 올바르지 않습니다." }

        val type = ActiveMeetingCursorType.valueOf(parts[0])

        return ActiveMeetingCursor(
            type = type,
            startDate = parts[1].takeUnless { it == EMPTY_DATE }?.let(LocalDate::parse),
            meetingId = parts[2].toLong(),
        )
    }
}
