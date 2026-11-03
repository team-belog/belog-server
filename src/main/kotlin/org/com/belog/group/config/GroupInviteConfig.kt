package org.com.belog.group.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@Configuration
@EnableConfigurationProperties(GroupInviteProperties::class)
class GroupInviteConfig
