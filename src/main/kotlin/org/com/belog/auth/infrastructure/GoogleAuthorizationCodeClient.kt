package org.com.belog.auth.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import org.com.belog.auth.code.AuthErrorCode
import org.com.belog.auth.config.GoogleAuthProperties
import org.com.belog.auth.config.GoogleTokenConfig
import org.com.belog.global.error.BusinessException
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException

@Component
class GoogleAuthorizationCodeClient(
    @Qualifier(GoogleTokenConfig.GOOGLE_OAUTH_REST_CLIENT)
    private val restClient: RestClient,
    private val properties: GoogleAuthProperties,
) {
    fun exchangeForIdToken(
        authorizationCode: String,
        redirectUri: String,
    ): String {
        if (authorizationCode.isBlank()) {
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_AUTHORIZATION_CODE)
        }
        if (redirectUri !in properties.redirectUris) {
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_REDIRECT_URI)
        }

        val response = requestToken(authorizationCode, redirectUri)
        if (response.idToken == null) {
            log.warn(
                "Google token response did not contain id_token: redirectUri={}. Ensure the openid scope is requested.",
                redirectUri,
            )
            throw BusinessException(AuthErrorCode.INVALID_GOOGLE_AUTHORIZATION_CODE)
        }
        return response.idToken
    }

    private fun requestToken(
        authorizationCode: String,
        redirectUri: String,
    ): GoogleTokenResponse {
        val formData =
            LinkedMultiValueMap<String, String>().apply {
                add("code", authorizationCode)
                add("client_id", properties.clientId)
                add("client_secret", properties.clientSecret)
                add("redirect_uri", redirectUri)
                add("grant_type", AUTHORIZATION_CODE_GRANT_TYPE)
            }

        try {
            return restClient
                .post()
                .uri(GOOGLE_TOKEN_PATH)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(GoogleTokenResponse::class.java)
                ?: throw BusinessException(AuthErrorCode.GOOGLE_AUTH_SERVER_ERROR)
        } catch (exception: RestClientResponseException) {
            if (exception.statusCode.is4xxClientError) {
                log.warn(
                    "Google token exchange rejected: status={}, error={}, redirectUri={}",
                    exception.statusCode,
                    extractGoogleError(exception.responseBodyAsString),
                    redirectUri,
                )
                throw BusinessException(AuthErrorCode.INVALID_GOOGLE_AUTHORIZATION_CODE, exception)
            }
            throw BusinessException(AuthErrorCode.GOOGLE_AUTH_SERVER_ERROR, exception)
        } catch (exception: RestClientException) {
            throw BusinessException(AuthErrorCode.GOOGLE_AUTH_SERVER_ERROR, exception)
        }
    }

    private data class GoogleTokenResponse(
        @field:JsonProperty("id_token")
        val idToken: String?,
    )

    companion object {
        private const val GOOGLE_TOKEN_PATH = "/token"
        private const val AUTHORIZATION_CODE_GRANT_TYPE = "authorization_code"
        private const val UNKNOWN_GOOGLE_ERROR = "unknown"
        private val GOOGLE_ERROR_PATTERN = Regex("\\\"error\\\"\\s*:\\s*\\\"([A-Za-z0-9_.-]+)\\\"")
        private val log = LoggerFactory.getLogger(GoogleAuthorizationCodeClient::class.java)

        private fun extractGoogleError(responseBody: String): String =
            GOOGLE_ERROR_PATTERN.find(responseBody)?.groupValues?.get(1) ?: UNKNOWN_GOOGLE_ERROR
    }
}
