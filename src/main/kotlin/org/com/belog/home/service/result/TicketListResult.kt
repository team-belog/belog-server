package org.com.belog.home.service.result

import org.com.belog.home.service.query.TicketCursor
import org.com.belog.postlog.service.result.PostLogTicketResult

data class TicketListResult(
    val items: List<PostLogTicketResult>,
    val nextCursor: TicketCursor?,
    val hasNext: Boolean,
)
