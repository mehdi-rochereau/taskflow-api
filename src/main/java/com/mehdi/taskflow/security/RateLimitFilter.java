package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting filter for authentication endpoints.
 *
 * <p>Protects against brute force attacks by limiting the number of requests
 * per IP address on sensitive authentication endpoints:</p>
 * <ul>
 *   <li>{@code POST /api/auth/login} — maximum 5 attempts per minute</li>
 *   <li>{@code POST /api/auth/refresh} — maximum 20 attempts per minute</li>
 *   <li>{@code POST /api/auth/register} — maximum 3 attempts per hour</li>
 * </ul>
 *
 * <p>Uses the Token Bucket algorithm via the Bucket4j library.
 * Each IP address gets its own bucket, stored in a {@link ConcurrentHashMap}.
 * When the bucket is empty, the request is rejected with a
 * {@code 429 Too Many Requests} response.</p>
 *
 * <p>The IP address is extracted from the {@code X-Forwarded-For} header
 * when available (reverse proxy scenario), falling back to
 * {@link HttpServletRequest#getRemoteAddr()}.</p>
 *
 * @see OncePerRequestFilter
 */
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    private final MessageService messageService;
    private final AuditService auditService;

    /**
     * Buckets for login attempts — 5 requests per minute per IP.
     */
    private final Map<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    /**
     * Buckets for refresh attempts — 20 requests per minute per IP.
     */
    private final Map<String, Bucket> refreshBuckets = new ConcurrentHashMap<>();

    /**
     * Buckets for registration attempts — 3 requests per hour per IP.
     */
    private final Map<String, Bucket> registerBuckets = new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code RateLimitFilter} with its required dependency.
     *
     * @param messageService utility component for resolving i18n messages
     * @param auditService   service for logging security audit events
     */
    public RateLimitFilter(MessageService messageService, AuditService auditService) {
        this.messageService = messageService;
        this.auditService = auditService;
    }

    /**
     * Applies rate limiting on authentication endpoints.
     *
     * <p>Requests to other endpoints pass through without rate limiting.</p>
     *
     * @param request     the incoming HTTP request
     * @param response    the HTTP response
     * @param filterChain the filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = extractIp(request);

        if ("POST".equals(method) && "/api/auth/login".equals(path)) {
            Bucket bucket = loginBuckets.computeIfAbsent(ip, k -> createLoginBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, request);
                return;
            }
        } else if ("POST".equals(method) && "/api/auth/refresh".equals(path)) {
            Bucket bucket = refreshBuckets.computeIfAbsent(ip, k -> createRefreshBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, request);
                return;
            }
        } else if ("POST".equals(method) && "/api/auth/register".equals(path)) {
            Bucket bucket = registerBuckets.computeIfAbsent(ip, k -> createRegisterBucket());
            if (!bucket.tryConsume(1)) {
                rejectRequest(response, request);
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Creates a bucket allowing 5 requests per minute.
     * Used for login endpoint protection.
     *
     * @return a configured {@link Bucket}
     */
    private Bucket createLoginBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(5)
                        .refillGreedy(5, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Creates a bucket allowing 20 requests per minute.
     * Used for refresh endpoint protection.
     *
     * @return a configured {@link Bucket}
     */
    private Bucket createRefreshBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(20)
                        .refillGreedy(20, Duration.ofMinutes(1))
                        .build())
                .build();
    }

    /**
     * Creates a bucket allowing 3 requests per hour.
     * Used for registration endpoint protection.
     *
     * @return a configured {@link Bucket}
     */
    private Bucket createRegisterBucket() {
        return Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(3)
                        .refillGreedy(3, Duration.ofHours(1))
                        .build())
                .build();
    }

    /**
     * Extracts the client IP address from the request.
     *
     * <p>Checks the {@code X-Forwarded-For} header first to handle
     * reverse proxy scenarios (Nginx, load balancer). Falls back to
     * {@link HttpServletRequest#getRemoteAddr()} if the header is absent.</p>
     *
     * @param request the incoming HTTP request
     * @return the client IP address
     */
    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Writes a {@code 429 Too Many Requests} JSON response and logs the rate limit violation.
     *
     * @param response the HTTP response to write to
     * @param request  the HTTP request used to extract the client IP for audit logging
     * @throws IOException if an I/O error occurs while writing the response
     */
    private void rejectRequest(HttpServletResponse response, HttpServletRequest request) throws IOException {
        auditService.logLoginFailure(extractIp(request));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(
                "{\"status\":429,\"message\":\""
                        + messageService.get("error.rate.limit.exceeded")
                        + "\"}"
        );
    }
}