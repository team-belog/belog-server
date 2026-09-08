package org.com.belog.global.error

import jakarta.validation.ConstraintViolationException
import org.com.belog.global.response.CommonResponse
import org.com.belog.global.response.code.CommonErrorCode
import org.com.belog.global.response.code.ErrorCode
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.BindingResult
import org.springframework.validation.method.ParameterErrors
import org.springframework.validation.method.ParameterValidationResult
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingRequestCookieException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.HandlerMethodValidationException
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(exception: BusinessException): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(exception.errorCode)

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(
        exception: MethodArgumentNotValidException,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.INVALID_INPUT, toFieldErrors(exception.bindingResult))

    @ExceptionHandler(BindException::class)
    fun handleBindException(exception: BindException): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.INVALID_INPUT, toFieldErrors(exception.bindingResult))

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(
        exception: ConstraintViolationException,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> {
        val fieldErrors = exception.constraintViolations.map { violation ->
            FieldErrorDetail(violation.propertyPath.toString(), violation.message)
        }

        return createResponse(CommonErrorCode.INVALID_INPUT, fieldErrors)
    }

    @ExceptionHandler(HandlerMethodValidationException::class)
    fun handleHandlerMethodValidation(
        exception: HandlerMethodValidationException,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> {
        val fieldErrors =
            exception.parameterValidationResults.flatMap(::toFieldErrors) +
                exception.crossParameterValidationResults.map { error ->
                    FieldErrorDetail("request", error.defaultMessage)
                }

        return createResponse(CommonErrorCode.INVALID_INPUT, fieldErrors)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        exception: MethodArgumentTypeMismatchException,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> {
        val fieldError = FieldErrorDetail(exception.name, "올바른 형식의 값을 입력해 주세요.")
        return createResponse(CommonErrorCode.INVALID_INPUT, listOf(fieldError))
    }

    @ExceptionHandler(
        MissingServletRequestParameterException::class,
        MissingRequestHeaderException::class,
        MissingRequestCookieException::class,
    )
    fun handleMissingRequestValue(): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.INVALID_INPUT)

    @ExceptionHandler(MissingServletRequestPartException::class)
    fun handleMissingServletRequestPart(
        exception: MissingServletRequestPartException,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> {
        val fieldError = FieldErrorDetail(exception.requestPartName, "필수 요청 파트입니다.")
        return createResponse(CommonErrorCode.INVALID_INPUT, listOf(fieldError))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.INVALID_REQUEST_BODY)

    @ExceptionHandler(NoHandlerFoundException::class, NoResourceFoundException::class)
    fun handleNoResourceFound(): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.RESOURCE_NOT_FOUND)

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.METHOD_NOT_ALLOWED)

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleMediaTypeNotSupported(): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE)

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleMediaTypeNotAcceptable(): ResponseEntity<Void> =
        ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build()

    @ExceptionHandler(Exception::class)
    fun handleUnexpectedException(exception: Exception): ResponseEntity<CommonResponse<ErrorMetadata>> {
        log.error("Unexpected server error", exception)
        return createResponse(CommonErrorCode.INTERNAL_SERVER_ERROR)
    }

    private fun createResponse(errorCode: ErrorCode): ResponseEntity<CommonResponse<ErrorMetadata>> =
        createResponse(errorCode, emptyList())

    private fun createResponse(
        errorCode: ErrorCode,
        fieldErrors: List<FieldErrorDetail>,
    ): ResponseEntity<CommonResponse<ErrorMetadata>> =
        ResponseEntity
            .status(errorCode.status)
            .body(CommonResponse.error(errorCode, ErrorMetadata.of(fieldErrors)))

    private fun toFieldErrors(bindingResult: BindingResult): List<FieldErrorDetail> =
        bindingResult.fieldErrors.map { error -> FieldErrorDetail(error.field, error.defaultMessage) }

    private fun toFieldErrors(result: ParameterValidationResult): List<FieldErrorDetail> {
        if (result is ParameterErrors) {
            return result.fieldErrors.map { error -> FieldErrorDetail(error.field, error.defaultMessage) }
        }

        val field = result.methodParameter.parameterName ?: "argument"
        return result.resolvableErrors.map { error -> FieldErrorDetail(field, error.defaultMessage) }
    }

    companion object {
        private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }
}
