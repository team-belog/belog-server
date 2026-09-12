package org.com.belog.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.cors")
data class CorsProperties(
    val allowedOrigins: List<String> = emptyList(),
    val allowedOriginPatterns: List<String> = emptyList(),
)
