package org.com.belog.global.error;

import jakarta.validation.ConstraintViolationException;
import java.util.List;
import java.util.stream.Stream;
import org.com.belog.global.response.CommonResponse;
import org.com.belog.global.response.code.CommonErrorCode;
import org.com.belog.global.response.code.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestCookieException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleBusinessException(BusinessException exception) {
        return createResponse(exception.getErrorCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception
    ) {
        return createResponse(
                CommonErrorCode.INVALID_INPUT,
                toFieldErrors(exception.getBindingResult())
        );
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleBindException(BindException exception) {
        return createResponse(
                CommonErrorCode.INVALID_INPUT,
                toFieldErrors(exception.getBindingResult())
        );
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleConstraintViolation(
            ConstraintViolationException exception
    ) {
        List<FieldErrorDetail> fieldErrors = exception.getConstraintViolations()
                .stream()
                .map(violation -> new FieldErrorDetail(
                        violation.getPropertyPath().toString(),
                        violation.getMessage()
                ))
                .toList();

        return createResponse(CommonErrorCode.INVALID_INPUT, fieldErrors);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleHandlerMethodValidation(
            HandlerMethodValidationException exception
    ) {
        List<FieldErrorDetail> fieldErrors = Stream.concat(
                exception.getParameterValidationResults().stream().flatMap(this::toFieldErrors),
                exception.getCrossParameterValidationResults().stream()
                        .map(error -> new FieldErrorDetail("request", error.getDefaultMessage()))
        ).toList();

        return createResponse(CommonErrorCode.INVALID_INPUT, fieldErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        FieldErrorDetail fieldError = new FieldErrorDetail(
                exception.getName(),
                "올바른 형식의 값을 입력해 주세요."
        );
        return createResponse(CommonErrorCode.INVALID_INPUT, List.of(fieldError));
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MissingRequestHeaderException.class,
            MissingRequestCookieException.class
    })
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMissingRequestValue() {
        return createResponse(CommonErrorCode.INVALID_INPUT);
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMissingServletRequestPart(
            MissingServletRequestPartException exception
    ) {
        FieldErrorDetail fieldError = new FieldErrorDetail(
                exception.getRequestPartName(),
                "필수 요청 파트입니다."
        );
        return createResponse(CommonErrorCode.INVALID_INPUT, List.of(fieldError));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleHttpMessageNotReadable() {
        return createResponse(CommonErrorCode.INVALID_REQUEST_BODY);
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleNoResourceFound() {
        return createResponse(CommonErrorCode.RESOURCE_NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMethodNotSupported() {
        return createResponse(CommonErrorCode.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleMediaTypeNotSupported() {
        return createResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Void> handleMediaTypeNotAcceptable() {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE).build();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<ErrorMetadata>> handleUnexpectedException(Exception exception) {
        log.error("Unexpected server error", exception);
        return createResponse(CommonErrorCode.INTERNAL_SERVER_ERROR);
    }

    private ResponseEntity<CommonResponse<ErrorMetadata>> createResponse(ErrorCode errorCode) {
        return createResponse(errorCode, List.of());
    }

    private ResponseEntity<CommonResponse<ErrorMetadata>> createResponse(
            ErrorCode errorCode,
            List<FieldErrorDetail> fieldErrors
    ) {
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(CommonResponse.error(errorCode, ErrorMetadata.of(fieldErrors)));
    }

    private List<FieldErrorDetail> toFieldErrors(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()))
                .toList();
    }

    private Stream<FieldErrorDetail> toFieldErrors(ParameterValidationResult result) {
        if (result instanceof ParameterErrors parameterErrors) {
            return parameterErrors.getFieldErrors().stream()
                    .map(error -> new FieldErrorDetail(error.getField(), error.getDefaultMessage()));
        }

        String parameterName = result.getMethodParameter().getParameterName();
        String field = parameterName != null ? parameterName : "argument";
        return result.getResolvableErrors().stream()
                .map(error -> new FieldErrorDetail(field, error.getDefaultMessage()));
    }
}
