package org.com.belog.postlog.service.result

import java.time.LocalDate

data class PostLogTicketResult(
    val ticketId: Long,
    val meetingId: Long,
    val meetingName: String,
    val memory: String,
    val coverPhotoUrl: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val location: String?,
    val participants: List<PostLogTicketParticipantResult>,
)

data class PostLogTicketParticipantResult(
    val groupMemberId: Long,
    val nickname: String,
)
