package org.com.belog.auth.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.com.belog.auth.controller.dto.GoogleLoginRequest
import org.com.belog.auth.controller.dto.GoogleLoginResponse
import org.com.belog.auth.controller.dto.TokenReissueResponse
import org.com.belog.global.openapi.CommonOpenApiExample
import org.com.belog.global.openapi.CommonOpenApiResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@Tag(name = "Auth", description = "인증 관련 API")
interface AuthSwagger {
    @Operation(
        summary = "Google 로그인",
        description = "Google 인가 코드를 검증하고 BELOG Access Token과 Refresh Token 쿠키를 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "Google 로그인 성공",
                useReturnTypeSchema = true,
                headers = [
                    Header(
                        name = "Set-Cookie",
                        description = "HttpOnly Refresh Token 쿠키",
                        schema =
                            Schema(
                                type = "string",
                                example = REFRESH_COOKIE_EXAMPLE,
                            ),
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(
                                name = "요청값 검증 실패",
                                ref = CommonOpenApiExample.INVALID_INPUT,
                            ),
                            ExampleObject(
                                name = "요청 본문 파싱 실패",
                                ref = CommonOpenApiExample.INVALID_REQUEST_BODY,
                            ),
                            ExampleObject(
                                name = "허용되지 않은 리디렉션 URI",
                                value = INVALID_REDIRECT_URI_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "401",
                description = "Google 인증 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [
                            ExampleObject(
                                name = "유효하지 않은 Google ID Token",
                                value = INVALID_GOOGLE_ID_TOKEN_EXAMPLE,
                            ),
                            ExampleObject(
                                name = "유효하지 않은 Google 인가 코드",
                                value = INVALID_GOOGLE_AUTHORIZATION_CODE_EXAMPLE,
                            ),
                        ],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "415",
                ref = CommonOpenApiResponse.UNSUPPORTED_MEDIA_TYPE,
            ),
            ApiResponse(
                responseCode = "502",
                description = "Google 인증 서버 통신 실패",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = GOOGLE_AUTH_SERVER_ERROR_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    @SecurityRequirements
    fun loginWithGoogle(request: GoogleLoginRequest): ResponseEntity<CommonResponse<GoogleLoginResponse>>

    @Operation(
        summary = "토큰 재발급",
        description = "Refresh Token 쿠키를 검증하고 새로운 Access Token과 Refresh Token을 발급합니다.",
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "토큰 재발급 성공",
                useReturnTypeSchema = true,
                headers = [
                    Header(
                        name = "Set-Cookie",
                        description = "새로운 HttpOnly Refresh Token 쿠키",
                        schema = Schema(type = "string", example = REFRESH_COOKIE_EXAMPLE),
                    ),
                ],
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [ExampleObject(value = TOKEN_REISSUE_SUCCESS_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "400",
                ref = CommonOpenApiResponse.MISSING_REQUEST_VALUE,
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 Refresh Token",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INVALID_REFRESH_TOKEN_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "403",
                description = "허용되지 않은 요청 출처",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INVALID_REQUEST_ORIGIN_EXAMPLE)],
                    ),
                ],
            ),
        ],
    )
    @SecurityRequirements
    fun reissueTokens(
        @Parameter(hidden = true) refreshToken: String,
    ): ResponseEntity<CommonResponse<TokenReissueResponse>>
}

private const val REFRESH_COOKIE_EXAMPLE =
    "refresh_token=eyJ...; Path=/api/v1/auth/refresh; Max-Age=1209600; " +
        "Secure; HttpOnly; SameSite=None"

private const val TOKEN_REISSUE_SUCCESS_EXAMPLE =
    """{"code":"AUTH-S002","message":"토큰 재발급에 성공했습니다.","data":{"accessToken":"eyJhbGciOiJIUzI1NiJ9...","expiresIn":1800}}"""

private const val INVALID_REFRESH_TOKEN_EXAMPLE =
    """{"code":"AUTH-E005","message":"유효하지 않거나 만료된 Refresh Token입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-11T08:00:00Z"}}"""

private const val INVALID_REQUEST_ORIGIN_EXAMPLE =
    """{"code":"AUTH-E006","message":"허용되지 않은 요청 출처입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-11T08:00:00Z"}}"""

private const val INVALID_REDIRECT_URI_EXAMPLE =
    """{"code":"AUTH-E004","message":"허용되지 않은 Google 리디렉션 URI입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_GOOGLE_ID_TOKEN_EXAMPLE =
    """{"code":"AUTH-E001","message":"유효하지 않은 Google 인증 정보입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_GOOGLE_AUTHORIZATION_CODE_EXAMPLE =
    """{"code":"AUTH-E002","message":"유효하지 않은 Google 인가 코드입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val GOOGLE_AUTH_SERVER_ERROR_EXAMPLE =
    """{"code":"AUTH-E003","message":"Google 인증 서버와 통신할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""
