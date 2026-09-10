package org.com.belog.auth.controller.dto

import jakarta.validation.constraints.NotBlank

data class GoogleLoginRequest(
    @field:NotBlank(message = "Google 인가 코드는 필수입니다.")
    val authorizationCode: String,
    @field:NotBlank(message = "Google 리디렉션 URI는 필수입니다.")
    val redirectUri: String,
)
