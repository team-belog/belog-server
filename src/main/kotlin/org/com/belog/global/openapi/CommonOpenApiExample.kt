package org.com.belog.global.openapi

object CommonOpenApiExample {
    private const val COMPONENT_EXAMPLE_PREFIX = "#/components/examples"

    internal const val INVALID_INPUT_NAME = "InvalidInput"
    internal const val INVALID_REQUEST_BODY_NAME = "InvalidRequestBody"

    const val INVALID_INPUT = "$COMPONENT_EXAMPLE_PREFIX/$INVALID_INPUT_NAME"
    const val INVALID_REQUEST_BODY = "$COMPONENT_EXAMPLE_PREFIX/$INVALID_REQUEST_BODY_NAME"
}
