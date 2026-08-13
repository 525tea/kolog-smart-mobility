package com.smbility.railcargo.common.exception;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ErrorResponse.builder()
                        .code(errorCode.name())
                        .message(e.getMessage())
                        .build());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldErrorDetail> fieldErrors = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldErrorDetail)
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INVALID_INPUT.name())
                        .message(ErrorCode.INVALID_INPUT.getDefaultMessage())
                        .fieldErrors(fieldErrors)
                        .build());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException e) {
        return ResponseEntity.status(ErrorCode.ACCESS_DENIED.getStatus())
                .body(ErrorResponse.builder()
                        .code(ErrorCode.ACCESS_DENIED.name())
                        .message(ErrorCode.ACCESS_DENIED.getDefaultMessage())
                        .build());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException e) {
        return ResponseEntity.status(ErrorCode.PAYLOAD_TOO_LARGE.getStatus())
                .body(ErrorResponse.builder()
                        .code(ErrorCode.PAYLOAD_TOO_LARGE.name())
                        .message("파일은 최대 20MB까지 업로드할 수 있습니다.")
                        .build());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(Exception e) {
        log.error("Unexpected exception", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.builder()
                        .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
                        .message(ErrorCode.INTERNAL_SERVER_ERROR.getDefaultMessage())
                        .build());
    }

    private ErrorResponse.FieldErrorDetail toFieldErrorDetail(FieldError fieldError) {
        return ErrorResponse.FieldErrorDetail.builder()
                .field(fieldError.getField())
                .reason(fieldError.getDefaultMessage())
                .build();
    }
}
