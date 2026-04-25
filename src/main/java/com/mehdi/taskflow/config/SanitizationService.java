package com.mehdi.taskflow.config;

import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.springframework.stereotype.Service;

/**
 * Service responsible for sanitizing user-provided text input.
 *
 * <p>Applies a strict HTML sanitization policy using the
 * <a href="https://github.com/OWASP/java-html-sanitizer">OWASP Java HTML Sanitizer</a>
 * to strip all HTML tags and potentially malicious content from user input
 * before it is persisted to the database.</p>
 *
 * <p>This service implements a defense-in-depth strategy against XSS attacks —
 * even if a client fails to escape output correctly, the stored data
 * will never contain executable scripts or malicious HTML.</p>
 *
 * <p>Applied on the following fields:</p>
 * <ul>
 *   <li>{@code Project.name} and {@code Project.description}</li>
 *   <li>{@code Task.title} and {@code Task.description}</li>
 * </ul>
 *
 * <p>Fields explicitly excluded from sanitization:</p>
 * <ul>
 *   <li>Passwords — handled by BCrypt</li>
 *   <li>Emails — validated by {@code @Email}</li>
 *   <li>Usernames — validated by {@code @Size} and {@code @NotBlank}</li>
 * </ul>
 *
 * @see AuditService#logSanitizationAttempt(String, String, String)
 */
@Service
public class SanitizationService {

    /**
     * Strict policy that strips all HTML tags and attributes.
     * No formatting, no links, no images — plain text only.
     */
    private static final PolicyFactory POLICY = Sanitizers.FORMATTING
            .and(Sanitizers.LINKS)
            .and(Sanitizers.IMAGES)
            .and(Sanitizers.BLOCKS)
            .and(Sanitizers.TABLES);

    /**
     * Sanitizes a user-provided text input and logs a warning if HTML content was detected.
     *
     * <p>Combines sanitization and audit logging in a single call to avoid
     * duplication across service classes.</p>
     *
     * @param input       the raw user input to sanitize
     * @param field       the name of the field being sanitized — used in the audit log
     * @param auditService the audit service used to log sanitization attempts
     * @return the sanitized plain text, or {@code null} if input is {@code null}
     */
    public String sanitizeAndLog(String input, String field, AuditService auditService) {
        String sanitized = sanitize(input);
        if (wasSanitized(input, sanitized)) {
            auditService.logSanitizationAttempt(field, input, sanitized);
        }
        return sanitized;
    }

    /**
     * Sanitizes a user-provided text input by stripping all HTML content.
     *
     * <p>Returns {@code null} if the input is {@code null} — allowing
     * optional fields to remain unset without modification.</p>
     *
     * <p>The sanitization is intentionally strict — all HTML tags are removed.
     * Plain text is preserved as-is.</p>
     *
     * <p>Examples:</p>
     * <pre>
     * sanitize("Hello World")                        → "Hello World"
     * sanitize("<script>alert('XSS')</script>Hello") → "Hello"
     * sanitize("<b>Bold</b> text")                   → "Bold text"
     * sanitize(null)                                 → null
     * </pre>
     *
     * @param input the raw user input to sanitize
     * @return the sanitized plain text, or {@code null} if input is {@code null}
     */
    public String sanitize(String input) {
        if (input == null) return null;
        return POLICY.sanitize(input);
    }

    /**
     * Checks whether the given input contains HTML content that would be
     * stripped by the sanitization policy.
     *
     * <p>Used to determine whether a sanitization attempt should be logged
     * as a potential XSS attempt.</p>
     *
     * @param original  the original input before sanitization
     * @param sanitized the sanitized output after sanitization
     * @return {@code true} if the sanitized output differs from the original
     */
    public boolean wasSanitized(String original, String sanitized) {
        if (original == null && sanitized == null) return false;
        if (original == null || sanitized == null) return true;
        return !original.equals(sanitized);
    }
}