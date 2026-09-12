package org.com.belog.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.examples.Example
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Content
import io.swagger.v3.oas.models.media.MediaType
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.responses.ApiResponse
import io.swagger.v3.oas.models.responses.ApiResponses
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import org.com.belog.global.response.code.CommonErrorCode
import org.springdoc.core.customizers.OpenApiCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE

@Configuration(proxyBeanMethods = false)
class OpenApiConfig {
    @Bean
    fun belogOpenApi(): OpenAPI =
        OpenAPI()
            .info(
                Info()
                    .title("BELOG API")
                    .description("BELOG 서비스 API 문서")
                    .version("v1"),
            ).components(commonComponents())
            .addSecurityItem(SecurityRequirement().addList(ACCESS_TOKEN_SECURITY_SCHEME))

    @Bean
    fun commonErrorResponseCustomizer(): OpenApiCustomizer =
        OpenApiCustomizer { openApi ->
            openApi.paths
                ?.values
                ?.flatMap { pathItem -> pathItem.readOperations() }
                ?.forEach { operation ->
                    val responses = operation.responses ?: ApiResponses().also(operation::setResponses)
                    GLOBAL_RESPONSES.forEach { (status, responseRef) ->
                        responses.putIfAbsent(status, ApiResponse().`$ref`(responseRef))
                    }
                }
        }

    private fun commonComponents(): Components =
        Components()
            .addSecuritySchemes(
                ACCESS_TOKEN_SECURITY_SCHEME,
                SecurityScheme()
                    .type(SecurityScheme.Type.HTTP)
                    .scheme("bearer")
                    .bearerFormat("JWT")
                    .description("BELOG Access Token을 입력해 주세요."),
            ).addExamples(
                CommonOpenApiExample.INVALID_INPUT_NAME,
                Example().value(
                    errorExample(
                        errorCode = CommonErrorCode.INVALID_INPUT,
                        fieldErrors =
                            listOf(
                                mapOf(
                                    "field" to "fieldName",
                                    "reason" to "올바른 값을 입력해 주세요.",
                                ),
                            ),
                    ),
                ),
            ).addExamples(
                CommonOpenApiExample.INVALID_REQUEST_BODY_NAME,
                Example().value(errorExample(CommonErrorCode.INVALID_REQUEST_BODY)),
            ).addResponses(
                CommonOpenApiResponse.INVALID_REQUEST_NAME,
                commonErrorResponseWithExamples(
                    description = "잘못된 요청",
                    "요청값 검증 실패" to CommonOpenApiExample.INVALID_INPUT,
                    "요청 본문 파싱 실패" to CommonOpenApiExample.INVALID_REQUEST_BODY,
                ),
            ).addResponses(
                CommonOpenApiResponse.MISSING_REQUEST_VALUE_NAME,
                commonErrorResponse(
                    description = "필수 요청값 누락",
                    errorCode = CommonErrorCode.INVALID_INPUT,
                ),
            ).addResponses(
                CommonOpenApiResponse.RESOURCE_NOT_FOUND_NAME,
                commonErrorResponse(
                    description = "요청한 리소스를 찾을 수 없음",
                    errorCode = CommonErrorCode.RESOURCE_NOT_FOUND,
                ),
            ).addResponses(
                CommonOpenApiResponse.METHOD_NOT_ALLOWED_NAME,
                commonErrorResponse(
                    description = "지원하지 않는 HTTP 메서드",
                    errorCode = CommonErrorCode.METHOD_NOT_ALLOWED,
                ),
            ).addResponses(
                CommonOpenApiResponse.NOT_ACCEPTABLE_NAME,
                ApiResponse().description("지원하지 않는 응답 형식 요청 — 응답 본문 없음"),
            ).addResponses(
                CommonOpenApiResponse.AUTHENTICATION_REQUIRED_NAME,
                commonErrorResponse(
                    description = "인증 필요",
                    errorCode = CommonErrorCode.AUTHENTICATION_REQUIRED,
                ),
            ).addResponses(
                CommonOpenApiResponse.ACCESS_DENIED_NAME,
                commonErrorResponse(
                    description = "접근 권한 없음",
                    errorCode = CommonErrorCode.ACCESS_DENIED,
                ),
            ).addResponses(
                CommonOpenApiResponse.UNSUPPORTED_MEDIA_TYPE_NAME,
                commonErrorResponse(
                    description = "지원하지 않는 Content-Type",
                    errorCode = CommonErrorCode.UNSUPPORTED_MEDIA_TYPE,
                ),
            ).addResponses(
                CommonOpenApiResponse.INTERNAL_SERVER_ERROR_NAME,
                commonErrorResponse(
                    description = "서버 내부 오류",
                    errorCode = CommonErrorCode.INTERNAL_SERVER_ERROR,
                ),
            )

    private fun commonErrorResponse(
        description: String,
        errorCode: CommonErrorCode,
    ): ApiResponse =
        ApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    APPLICATION_JSON_VALUE,
                    MediaType()
                        .schema(Schema<Any>().`$ref`(COMMON_RESPONSE_SCHEMA_REF))
                        .example(errorExample(errorCode)),
                ),
            )

    private fun commonErrorResponseWithExamples(
        description: String,
        vararg examples: Pair<String, String>,
    ): ApiResponse =
        ApiResponse()
            .description(description)
            .content(
                Content().addMediaType(
                    APPLICATION_JSON_VALUE,
                    MediaType()
                        .schema(Schema<Any>().`$ref`(COMMON_RESPONSE_SCHEMA_REF))
                        .examples(
                            examples.associate { (name, exampleRef) ->
                                name to Example().`$ref`(exampleRef)
                            },
                        ),
                ),
            )

    private fun errorExample(
        errorCode: CommonErrorCode,
        fieldErrors: List<Map<String, String>> = emptyList(),
    ): Map<String, Any> =
        mapOf(
            "code" to errorCode.code,
            "message" to errorCode.message,
            "data" to
                mapOf(
                    "fieldErrors" to fieldErrors,
                    "timestamp" to EXAMPLE_TIMESTAMP,
                ),
        )

    companion object {
        private const val COMMON_RESPONSE_SCHEMA_REF = "#/components/schemas/CommonResponse"
        private const val ACCESS_TOKEN_SECURITY_SCHEME = "AccessToken"
        private const val EXAMPLE_TIMESTAMP = "2026-09-13T00:00:00Z"
        private val GLOBAL_RESPONSES =
            mapOf(
                "405" to CommonOpenApiResponse.METHOD_NOT_ALLOWED,
                "406" to CommonOpenApiResponse.NOT_ACCEPTABLE,
                "500" to CommonOpenApiResponse.INTERNAL_SERVER_ERROR,
            )
    }
}
