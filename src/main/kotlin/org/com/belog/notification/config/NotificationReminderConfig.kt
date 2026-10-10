package org.com.belog.notification.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(NotificationReminderProperties::class)
class NotificationReminderConfig
