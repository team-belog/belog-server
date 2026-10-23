package org.com.belog.home.service.result

import org.com.belog.home.service.query.TicketCursor
import java.time.LocalDate

data class TicketListResult(
    val items: List<TicketResult>,
    val nextCursor: TicketCursor?,
    val hasNext: Boolean,
)

data class TicketResult(
    val ticketId: Long,
    val name: String,
    val memory: String,
    val coverPhotoUrl: String?,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val location: String?,
    val participantCount: Int,
    val participants: List<TicketParticipantResult>,
)

data class TicketParticipantResult(
    val groupMemberId: Long,
    val nickname: String,
)
