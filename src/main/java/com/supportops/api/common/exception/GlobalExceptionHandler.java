package com.supportops.api.common.exception;

import com.supportops.api.common.dto.ApiErrorDetail;
import com.supportops.api.common.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        ApiErrorDetail detail = new ApiErrorDetail(
            ex.getCode(),
            ex.getMessage(),
            null,
            resolveTraceId(request));

        return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponse.of(detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();

        ApiErrorDetail detail = new ApiErrorDetail(
            "VALIDATION_ERROR",
            "Validation failed",
            details,
            resolveTraceId(request));

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiErrorResponse.of(detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        ApiErrorDetail detail = new ApiErrorDetail(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            null,
            resolveTraceId(request));

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of(detail));
    }

    private String resolveTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("x-trace-id");
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
