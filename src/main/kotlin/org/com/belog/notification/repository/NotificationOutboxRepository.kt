package org.com.belog.notification.repository

import org.com.belog.notification.domain.NotificationOutbox
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationOutboxRepository : JpaRepository<NotificationOutbox, Long>
