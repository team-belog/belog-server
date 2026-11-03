package org.com.belog.auth.controller.dto

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "Google 로그인 요청")
data class GoogleLoginRequest(
    @field:NotBlank(message = "Google 인가 코드는 필수입니다.")
    @field:Schema(description = "Google에서 발급받은 일회용 인가 코드", example = "4/0AbCdEf...")
    val authorizationCode: String,
    @field:NotBlank(message = "Google 리디렉션 URI는 필수입니다.")
    @field:Schema(description = "인가 코드 발급에 사용한 리디렉션 URI", example = "http://localhost:3000/oauth/google/callback")
    val redirectUri: String,
)
