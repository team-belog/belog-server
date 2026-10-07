package org.com.belog.postlog.repository

import org.com.belog.postlog.domain.PostLogTicket
import org.springframework.data.jpa.repository.JpaRepository

interface PostLogTicketRepository : JpaRepository<PostLogTicket, Long>
