package com.mehdi.taskflow.exception;

import com.mehdi.taskflow.config.MessageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Centralized exception handler for the entire REST API.
 *
 * <p>Intercepts exceptions thrown by controllers and services,
 * and transforms them into structured JSON HTTP responses.</p>
 *
 * <p>Standard error response structure:</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-04-08T10:00:00",
 *   "status": 404,
 *   "message": "Project not found"
 * }
 * }</pre>
 *
 * <p>Validation error response structure ({@code 400}):</p>
 * <pre>{@code
 * {
 *   "timestamp": "2026-04-08T10:00:00",
 *   "status": 400,
 *   "errors": {
 *     "name": "Project name is required"
 *   }
 * }
 * }</pre>
 *
 * @see ResourceNotFoundException
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private final MessageService messageService;

    /**
     * Constructs a new {@code GlobalExceptionHandler} with its required dependencies.
     *
     * @param messageService utility component for resolving i18n messages based on the current request locale
     */
    public GlobalExceptionHandler(MessageService messageService) {
        this.messageService = messageService;
    }

    /**
     * Handles resource not found exceptions.
     *
     * @param ex exception carrying the descriptive message
     * @return {@code 404 Not Found} response with the error message
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFound(
            ResourceNotFoundException ex) {
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /**
     * Handles unauthorized access to a protected resource.
     *
     * @param ex access denied exception
     * @return {@code 403 Forbidden} response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<Map<String, Object>> handleAccessDenied(
            AccessDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, messageService.get("error.access.denied"));
    }

    /**
     * Handles business logic violations such as duplicate username or email.
     *
     * @param ex exception carrying the business error message
     * @return {@code 400 Bad Request} response with the error message
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /**
     * Handles validation errors for DTOs annotated with {@code @Valid}.
     * Returns a detailed map of field-level constraint violations.
     *
     * @param ex exception containing the list of constraint violations
     * @return {@code 400 Bad Request} response with a map of field errors
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(
            MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String field = ((FieldError) error).getField();
            String message = error.getDefaultMessage();
            errors.put(field, message);
        });
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("errors", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    /**
     * Fallback handler for any unexpected exception not covered by other handlers.
     * Prevents internal error details from being exposed to the client.
     *
     * @param ex unexpected exception
     * @return {@code 500 Internal Server Error} response with a generic message
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGeneric(Exception ex) {
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                messageService.get("error.unexpected"));
    }

    /**
     * Handles type mismatch errors in path variables or request parameters.
     * For example, passing a non-numeric value where a {@code Long} is expected.
     *
     * @param ex exception containing the mismatched parameter details
     * @return {@code 400 Bad Request} response with a descriptive error message
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(
            MethodArgumentTypeMismatchException ex) {
        String message = messageService.get(
                "error.parameter.type.mismatch",
                ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown"
        );
        return buildResponse(HttpStatus.BAD_REQUEST, message);
    }

    /**
     * Builds a standardized error response body.
     *
     * @param status  HTTP status code
     * @param message error message to include in the response body
     * @return {@link ResponseEntity} with the structured error body
     */
    private ResponseEntity<Map<String, Object>> buildResponse(
            HttpStatus status, String message) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}