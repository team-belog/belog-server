package org.com.belog.global.openapi

object CommonOpenApiResponse {
    private const val COMPONENT_RESPONSE_PREFIX = "#/components/responses"

    internal const val INVALID_REQUEST_NAME = "InvalidRequest"
    internal const val MISSING_REQUEST_VALUE_NAME = "MissingRequestValue"
    internal const val RESOURCE_NOT_FOUND_NAME = "ResourceNotFound"
    internal const val METHOD_NOT_ALLOWED_NAME = "MethodNotAllowed"
    internal const val NOT_ACCEPTABLE_NAME = "NotAcceptable"
    internal const val AUTHENTICATION_REQUIRED_NAME = "AuthenticationRequired"
    internal const val ACCESS_DENIED_NAME = "AccessDenied"
    internal const val UNSUPPORTED_MEDIA_TYPE_NAME = "UnsupportedMediaType"
    internal const val INTERNAL_SERVER_ERROR_NAME = "InternalServerError"

    const val INVALID_REQUEST = "$COMPONENT_RESPONSE_PREFIX/$INVALID_REQUEST_NAME"
    const val MISSING_REQUEST_VALUE = "$COMPONENT_RESPONSE_PREFIX/$MISSING_REQUEST_VALUE_NAME"
    const val RESOURCE_NOT_FOUND = "$COMPONENT_RESPONSE_PREFIX/$RESOURCE_NOT_FOUND_NAME"
    const val METHOD_NOT_ALLOWED = "$COMPONENT_RESPONSE_PREFIX/$METHOD_NOT_ALLOWED_NAME"
    const val NOT_ACCEPTABLE = "$COMPONENT_RESPONSE_PREFIX/$NOT_ACCEPTABLE_NAME"
    const val AUTHENTICATION_REQUIRED = "$COMPONENT_RESPONSE_PREFIX/$AUTHENTICATION_REQUIRED_NAME"
    const val ACCESS_DENIED = "$COMPONENT_RESPONSE_PREFIX/$ACCESS_DENIED_NAME"
    const val UNSUPPORTED_MEDIA_TYPE = "$COMPONENT_RESPONSE_PREFIX/$UNSUPPORTED_MEDIA_TYPE_NAME"
    const val INTERNAL_SERVER_ERROR = "$COMPONENT_RESPONSE_PREFIX/$INTERNAL_SERVER_ERROR_NAME"
}
