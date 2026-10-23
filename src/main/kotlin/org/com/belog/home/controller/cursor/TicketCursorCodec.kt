package org.com.belog.home.controller.cursor

import org.com.belog.home.service.query.TicketCursor
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.util.Base64

internal object TicketCursorCodec {
    private const val SEPARATOR = ":"

    fun encode(cursor: TicketCursor): String {
        val rawCursor = "${cursor.endDate}$SEPARATOR${cursor.ticketId}"
        return Base64.getUrlEncoder().withoutPadding().encodeToString(rawCursor.toByteArray(StandardCharsets.UTF_8))
    }

    fun decode(value: String?): TicketCursor? {
        if (value == null) {
            return null
        }

        return decodeHomeCursor {
            val decodedValue = String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8)
            val parts = decodedValue.split(SEPARATOR)
            require(parts.size == 2) { "티켓 목록 커서 형식이 올바르지 않습니다." }

            TicketCursor(
                endDate = LocalDate.parse(parts[0]),
                ticketId = parts[1].toLong(),
            )
        }
    }
}
