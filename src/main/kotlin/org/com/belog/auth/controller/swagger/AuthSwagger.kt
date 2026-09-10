package org.com.belog.auth.controller.swagger

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.headers.Header
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.com.belog.auth.controller.dto.GoogleLoginRequest
import org.com.belog.auth.controller.dto.GoogleLoginResponse
import org.com.belog.global.response.CommonResponse
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity

@Tag(name = "인증", description = "로그인과 토큰 관련 API")
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
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        examples = [
                            ExampleObject(
                                name = "로그인 성공",
                                value = GOOGLE_LOGIN_SUCCESS_EXAMPLE,
                            ),
                        ],
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
                                value = INVALID_INPUT_EXAMPLE,
                            ),
                            ExampleObject(
                                name = "요청 본문 파싱 실패",
                                value = INVALID_REQUEST_BODY_EXAMPLE,
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
                responseCode = "405",
                description = "지원하지 않는 HTTP 메서드",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = METHOD_NOT_ALLOWED_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "406",
                description = "지원하지 않는 응답 형식 요청 — 응답 본문 없음",
                content = [Content()],
            ),
            ApiResponse(
                responseCode = "415",
                description = "지원하지 않는 Content-Type",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = UNSUPPORTED_MEDIA_TYPE_EXAMPLE)],
                    ),
                ],
            ),
            ApiResponse(
                responseCode = "500",
                description = "서버 내부 오류",
                content = [
                    Content(
                        mediaType = MediaType.APPLICATION_JSON_VALUE,
                        schema = Schema(implementation = CommonResponse::class),
                        examples = [ExampleObject(value = INTERNAL_SERVER_ERROR_EXAMPLE)],
                    ),
                ],
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
    fun loginWithGoogle(request: GoogleLoginRequest): ResponseEntity<CommonResponse<GoogleLoginResponse>>
}

private const val GOOGLE_LOGIN_SUCCESS_EXAMPLE =
    """{"code":"AUTH-S001","message":"Google 로그인에 성공했습니다.","data":{"accessToken":"eyJhbGciOiJIUzI1NiJ9...","expiresIn":1800,"onboardingRequired":true}}"""

private const val REFRESH_COOKIE_EXAMPLE =
    "refresh_token=eyJ...; Path=/api/v1/auth/refresh; Max-Age=1209600; " +
        "Secure; HttpOnly; SameSite=Lax"

private const val INVALID_INPUT_EXAMPLE =
    """{"code":"CMN-E001","message":"요청값이 올바르지 않습니다.","data":{"fieldErrors":[{"field":"authorizationCode","reason":"Google 인가 코드는 필수입니다."}],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_REQUEST_BODY_EXAMPLE =
    """{"code":"CMN-E002","message":"요청 본문을 읽을 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_REDIRECT_URI_EXAMPLE =
    """{"code":"AUTH-E004","message":"허용되지 않은 Google 리디렉션 URI입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_GOOGLE_ID_TOKEN_EXAMPLE =
    """{"code":"AUTH-E001","message":"유효하지 않은 Google 인증 정보입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INVALID_GOOGLE_AUTHORIZATION_CODE_EXAMPLE =
    """{"code":"AUTH-E002","message":"유효하지 않은 Google 인가 코드입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val GOOGLE_AUTH_SERVER_ERROR_EXAMPLE =
    """{"code":"AUTH-E003","message":"Google 인증 서버와 통신할 수 없습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val METHOD_NOT_ALLOWED_EXAMPLE =
    """{"code":"CMN-E004","message":"지원하지 않는 HTTP 메서드입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val UNSUPPORTED_MEDIA_TYPE_EXAMPLE =
    """{"code":"CMN-E007","message":"지원하지 않는 Content-Type입니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""

private const val INTERNAL_SERVER_ERROR_EXAMPLE =
    """{"code":"CMN-E999","message":"서버 내부 오류가 발생했습니다.","data":{"fieldErrors":[],"timestamp":"2026-09-10T08:00:00Z"}}"""
