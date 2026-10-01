package org.com.belog.notification.repository

import org.com.belog.notification.domain.Notification

interface NotificationRepositoryCustom {
    fun saveIfAbsent(notification: Notification)
}
