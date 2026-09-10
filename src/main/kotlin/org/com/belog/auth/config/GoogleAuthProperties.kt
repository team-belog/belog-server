package org.com.belog.auth.config

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated

@Validated
@ConfigurationProperties(prefix = "auth.google")
data class GoogleAuthProperties(
    @field:NotBlank
    val clientId: String,
    @field:NotBlank
    val clientSecret: String,
    @field:NotEmpty
    val redirectUris: Set<String>,
)
