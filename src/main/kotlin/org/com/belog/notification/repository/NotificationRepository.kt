package org.com.belog.notification.repository

import org.com.belog.notification.domain.Notification
import org.springframework.data.jpa.repository.JpaRepository

interface NotificationRepository :
    JpaRepository<Notification, Long>,
    NotificationRepositoryCustom
