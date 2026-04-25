package com.mehdi.taskflow.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Service responsible for logging security-relevant audit events.
 *
 * <p>Centralizes all audit logging to ensure consistent formatting
 * and a single point of control for security event tracing.</p>
 *
 * <p>All events are logged at {@code INFO} level using a dedicated
 * logger named {@code AUDIT} — allowing audit logs to be routed
 * to a separate file or monitoring system via Logback configuration.</p>
 *
 * <p>Logged events include:</p>
 * <ul>
 *   <li>Successful authentication</li>
 *   <li>Failed authentication attempts</li>
 *   <li>Account registration</li>
 *   <li>Sensitive data deletion (projects, tasks)</li>
 * </ul>
 *
 * <p>Each log entry includes the client IP address extracted from the
 * current HTTP request context via {@link RequestContextHolder}.</p>
 *
 * @see org.slf4j.Logger
 */
@Service
public class AuditService {

    private static final Logger AUDIT = LoggerFactory.getLogger("AUDIT");

    /**
     * Logs a successful login event.
     *
     * @param username the authenticated user's username
     */
    public void logLoginSuccess(String username) {
        AUDIT.info("[LOGIN_SUCCESS] username={} ip={}", username, extractIp());
    }

    /**
     * Logs a failed login attempt.
     *
     * @param identifier the identifier used in the failed attempt (username or email)
     */
    public void logLoginFailure(String identifier) {
        AUDIT.warn("[LOGIN_FAILURE] identifier={} ip={}", identifier, extractIp());
    }

    /**
     * Logs a successful account registration.
     *
     * @param username the newly registered user's username
     */
    public void logRegisterSuccess(String username) {
        AUDIT.info("[REGISTER_SUCCESS] username={} ip={}", username, extractIp());
    }

    /**
     * Logs a project deletion event.
     *
     * @param projectId the identifier of the deleted project
     * @param username  the username of the user who performed the deletion
     */
    public void logProjectDeletion(Long projectId, String username) {
        AUDIT.info("[PROJECT_DELETE] projectId={} username={} ip={}", projectId, username, extractIp());
    }

    /**
     * Logs a task deletion event.
     *
     * @param taskId   the identifier of the deleted task
     * @param username the username of the user who performed the deletion
     */
    public void logTaskDeletion(Long taskId, String username) {
        AUDIT.info("[TASK_DELETE] taskId={} username={} ip={}", taskId, username, extractIp());
    }

    /**
     * Logs an unexpected error for debugging and monitoring purposes.
     *
     * <p>Called by {@link com.mehdi.taskflow.exception.GlobalExceptionHandler}
     * as a fallback for any unhandled exception. Logs the exception type
     * and message without exposing internal details to the client.</p>
     *
     * @param ex the unexpected exception
     */
    public void logUnexpectedError(Exception ex) {
        AUDIT.error("[UNEXPECTED_ERROR] type={} message={}",
                ex.getClass().getSimpleName(), ex.getMessage());
    }

    /**
     * Logs a sanitization attempt when user-provided input contained HTML content.
     *
     * <p>Called when the sanitized output differs from the original input,
     * indicating a potential XSS attempt or accidental HTML input.</p>
     *
     * @param field     the name of the field that was sanitized (e.g. "name", "title")
     * @param original  the original input before sanitization
     * @param sanitized the sanitized output after sanitization
     */
    public void logSanitizationAttempt(String field, String original, String sanitized) {
        AUDIT.warn("[SANITIZATION] field={} ip={} original={} sanitized={}",
                field, extractIp(), original, sanitized);
    }

    /**
     * Logs a scheduled token purge event.
     *
     * @param count the number of tokens deleted during the purge
     */
    public void logTokenPurge(int count) {
        AUDIT.info("[TOKEN_PURGE] deleted={}", count);
    }

    /**
     * Extracts the client IP address from the current HTTP request context.
     *
     * <p>Checks the {@code X-Forwarded-For} header first to handle
     * reverse proxy scenarios. Falls back to {@code getRemoteAddr()}
     * if the header is absent or empty.</p>
     *
     * @return the client IP address, or {@code "unknown"} if no request context is available
     */
    private String extractIp() {
        try {
            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) return "unknown";
            HttpServletRequest request = attributes.getRequest();
            String forwarded = request.getHeader("X-Forwarded-For");
            if (forwarded != null && !forwarded.isEmpty()) {
                return forwarded.split(",")[0].trim();
            }
            return request.getRemoteAddr();
        } catch (Exception e) {
            return "unknown";
        }
    }
}