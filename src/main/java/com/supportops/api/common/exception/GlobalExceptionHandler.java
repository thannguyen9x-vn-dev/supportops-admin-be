package com.supportops.api.common.exception;

import com.supportops.api.common.dto.ApiErrorDetail;
import com.supportops.api.common.dto.ApiErrorResponse;
import com.supportops.api.common.filter.TraceIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiErrorResponse> handleAppException(AppException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.warn("[{}] AppException: {} - {}", traceId, ex.getCode(), ex.getMessage());

        ApiErrorDetail detail = new ApiErrorDetail(
            ex.getCode(),
            ex.getMessage(),
            null,
            traceId);

        return ResponseEntity.status(ex.getStatus()).body(ApiErrorResponse.of(detail));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
            .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
            .toList();

        ApiErrorDetail detail = new ApiErrorDetail(
            "VALIDATION_ERROR",
            "Validation failed",
            details,
            traceId);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiErrorResponse.of(detail));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnknown(Exception ex, HttpServletRequest request) {
        String traceId = resolveTraceId(request);
        log.error("[{}] Unexpected error", traceId, ex);

        ApiErrorDetail detail = new ApiErrorDetail(
            "INTERNAL_ERROR",
            "An unexpected error occurred",
            null,
            traceId);

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiErrorResponse.of(detail));
    }

    private String resolveTraceId(HttpServletRequest request) {
        Object traceAttr = request.getAttribute(TraceIdFilter.TRACE_ATTR);
        if (traceAttr instanceof String traceIdFromAttr && !traceIdFromAttr.isBlank()) {
            return traceIdFromAttr;
        }

        String traceId = request.getHeader(TraceIdFilter.TRACE_HEADER);
        if (traceId == null || traceId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return traceId;
    }
}
