package com.store.ecommerce.exception;

import com.store.ecommerce.config.ratelimit.RateLimitExceededException;
import com.store.ecommerce.dto.response.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // ===== CUSTOM EXCEPTION =====
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", ex.getMessage(), request, ex);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(ConflictException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "RESOURCE_CONFLICT", ex.getMessage(), request, ex);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> handleBadRequest(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_ARGUMENT", ex.getMessage(), request, ex);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleRateLimit(RateLimitExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMIT_EXCEEDED", ex.getMessage(), request, ex);
    }

    // ===== VALIDATION =====
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        // Gom tất cả các lỗi validation lại thay vì chỉ lấy lỗi đầu tiên
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(" | "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, ex);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex, HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining(" | "));
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request, ex);
    }

    // ===== DATABASE =====
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        String message = extractDbErrorMessage(ex);
        return buildResponse(HttpStatus.CONFLICT, "DATA_INTEGRITY_ERROR", message, request, ex);
    }

    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ApiErrorResponse> handleOptimisticLock(OptimisticLockingFailureException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_ERROR",
                "Resource has been modified by another transaction. Please refresh and try again.", request, ex);
    }

    @ExceptionHandler(TransactionSystemException.class)
    public ResponseEntity<ApiErrorResponse> handleTransaction(TransactionSystemException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "TRANSACTION_ERROR",
                "Transaction failed. Please try again.", request, ex);
    }

    // ===== REQUEST FORMAT & MISSING DATA =====
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleJsonError(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Invalid JSON format", request, ex);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "Missing parameter: " + ex.getParameterName(), request, ex);
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ApiErrorResponse> handleMissingPathVariable(MissingPathVariableException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.BAD_REQUEST, "MISSING_PATH_VARIABLE", "Missing path variable: " + ex.getVariableName(), request, ex);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String message = "Invalid value for parameter '" + ex.getName() + "': expected " +
                (ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "different type");
        return buildResponse(HttpStatus.BAD_REQUEST, "TYPE_MISMATCH", message, request, ex);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponse> handleMaxUploadSize(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE",
                "File size exceeds maximum allowed limit", request, ex);
    }

    // ===== ROUTING & METHOD =====
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponse> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_SUPPORTED", ex.getMessage(), request, ex);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNoResourceFound(NoResourceFoundException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.NOT_FOUND, "ENDPOINT_NOT_FOUND",
                "Endpoint not found: " + request.getRequestURI(), request, ex);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponse> handleResponseStatusException(ResponseStatusException ex, HttpServletRequest request) {
        // Chuyển đổi HttpStatusCode sang HttpStatus
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildResponse(status, "RESPONSE_STATUS_ERROR", ex.getReason(), request, ex);
    }

    // ===== SECURITY =====
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "You do not have permission to access this resource", request, ex);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "AUTHENTICATION_FAILED",
                "Invalid credentials or authentication required", request, ex);
    }

    @ExceptionHandler(CsrfException.class)
    public ResponseEntity<ApiErrorResponse> handleCsrf(CsrfException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "CSRF_ERROR", "CSRF token missing or invalid", request, ex);
    }

    // ===== IO EXCEPTIONS =====
    @ExceptionHandler(java.io.IOException.class)
    public ResponseEntity<ApiErrorResponse> handleIOException(java.io.IOException ex, HttpServletRequest request) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "IO_ERROR", "Failed to process file operation", request, ex);
    }

    // ===== GENERAL EXCEPTION (FALLBACK) =====
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneral(Exception ex, HttpServletRequest request) {
        String traceId = UUID.randomUUID().toString();
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "UNEXPECTED_ERROR",
                "An unexpected error occurred. Please contact support with trace ID: " + traceId,
                request, traceId, ex
        );
    }

    // ==========================================
    // ========== COMMON BUILDER LOGIC ==========
    // ==========================================

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String errorCode, String message, HttpServletRequest request, Exception ex) {
        return buildResponse(status, errorCode, message, request, UUID.randomUUID().toString(), ex);
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatus status, String errorCode, String message, HttpServletRequest request, String traceId, Exception ex) {

        ApiErrorResponse response = ApiErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(errorCode)
                .message(message)
                .path(request.getRequestURI())
                .traceId(traceId)
                .build();

        if (status.is5xxServerError()) {
            log.error("Error [{}]: {} at {} | TraceId: {}", errorCode, message, request.getRequestURI(), traceId, ex);
        } else {
            log.warn("Error [{}]: {} at {} | TraceId: {}", errorCode, message, request.getRequestURI(), traceId);
        }

        return new ResponseEntity<>(response, status);
    }

    private String extractDbErrorMessage(DataIntegrityViolationException ex) {
        Throwable cause = ex.getMostSpecificCause();

        if (cause instanceof SQLException sqlEx) {
            if (sqlEx.getSQLState() != null && sqlEx.getSQLState().startsWith("23")) {
                return "Duplicate record exists or data constraint violated";
            }

            if (sqlEx.getErrorCode() == 1062) {
                return "Duplicate record exists";
            }
        }

        String msg = cause.getMessage() != null ? cause.getMessage().toLowerCase() : "";
        if (msg.contains("duplicate")) return "Duplicate record exists";
        if (msg.contains("foreign key")) return "Cannot delete or update due to foreign key constraint";
        if (msg.contains("not null")) return "Required field cannot be null";

        return "Database operation failed";
    }
}