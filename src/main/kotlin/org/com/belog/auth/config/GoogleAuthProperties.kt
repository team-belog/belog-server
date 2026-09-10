package org.com.belog.auth.config

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "auth.google")
data class GoogleAuthProperties(
    @field:NotBlank
    val clientId: String,
)
