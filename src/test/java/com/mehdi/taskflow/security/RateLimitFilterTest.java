package com.mehdi.taskflow.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class RateLimitFilterTest {

    @Mock private MessageService messageService;

    @Mock private AuditService auditService;

    @Mock private FilterChain filterChain;

    private RateLimitFilter rateLimitFilter;

    @BeforeEach
    void setUp() {
        // The buckets are instance fields, not static ones, so a fresh filter
        // starts with fresh counters. Without that, one test exhausting a bucket
        // would leave the next one throttled and the order would matter.
        rateLimitFilter = new RateLimitFilter(messageService, auditService);

        // Only resolved on the rejection path, which most tests never reach.
        lenient()
                .when(messageService.get("error.rate.limit.exceeded"))
                .thenReturn("Too many requests. Please try again later.");
    }

    /** Sends one request through the filter and returns the response produced. */
    private MockHttpServletResponse send(String method, String path, String ip)
            throws IOException, jakarta.servlet.ServletException {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setRemoteAddr(ip);
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilter(request, response, filterChain);
        return response;
    }

    // ── Login: 5 per minute ──────────────────────────────────────────

    @Test
    void login_shouldAllowFiveRequests_thenRejectTheSixth() throws Exception {
        // GIVEN
        // SECURITY.md advertises five login attempts per minute per IP. This is
        // the test that makes that claim verifiable rather than merely stated.

        // WHEN
        for (int i = 1; i <= 5; i++) {
            assertEquals(
                    200, send("POST", "/api/auth/login", "203.0.113.7").getStatus(), "call " + i);
        }
        MockHttpServletResponse sixth = send("POST", "/api/auth/login", "203.0.113.7");

        // THEN
        assertEquals(429, sixth.getStatus());
        // The first five reached the chain, the sixth did not: a filter that
        // returned 429 while still forwarding the request would let the brute
        // force through behind an error page.
        verify(filterChain, times(5)).doFilter(any(), any());
    }

    // ── Refresh: 20 per minute ───────────────────────────────────────

    @Test
    void refresh_shouldAllowTwentyRequests_thenRejectTheTwentyFirst() throws Exception {
        // WHEN
        for (int i = 1; i <= 20; i++) {
            assertEquals(
                    200, send("POST", "/api/auth/refresh", "203.0.113.7").getStatus(), "call " + i);
        }
        MockHttpServletResponse extra = send("POST", "/api/auth/refresh", "203.0.113.7");

        // THEN
        assertEquals(429, extra.getStatus());
        verify(filterChain, times(20)).doFilter(any(), any());
    }

    // ── Register: 3 per hour ─────────────────────────────────────────

    @Test
    void register_shouldAllowThreeRequests_thenRejectTheFourth() throws Exception {
        // WHEN
        for (int i = 1; i <= 3; i++) {
            assertEquals(
                    200,
                    send("POST", "/api/auth/register", "203.0.113.7").getStatus(),
                    "call " + i);
        }
        MockHttpServletResponse fourth = send("POST", "/api/auth/register", "203.0.113.7");

        // THEN
        assertEquals(429, fourth.getStatus());
        verify(filterChain, times(3)).doFilter(any(), any());
    }

    // ── Bucket isolation ─────────────────────────────────────────────

    @Test
    void buckets_shouldBeIndependentPerIp() throws Exception {
        // GIVEN
        // One attacker must not be able to lock every other user out of the
        // login endpoint by exhausting a shared counter.
        for (int i = 1; i <= 5; i++) {
            send("POST", "/api/auth/login", "203.0.113.7");
        }

        // WHEN
        MockHttpServletResponse otherIp = send("POST", "/api/auth/login", "198.51.100.2");

        // THEN
        assertEquals(200, otherIp.getStatus());
    }

    @Test
    void buckets_shouldBeIndependentPerEndpoint() throws Exception {
        // GIVEN
        // Exhausting login must not consume the register allowance: three
        // separate maps exist precisely so the limits do not interfere.
        for (int i = 1; i <= 5; i++) {
            send("POST", "/api/auth/login", "203.0.113.7");
        }

        // WHEN
        MockHttpServletResponse register = send("POST", "/api/auth/register", "203.0.113.7");

        // THEN
        assertEquals(200, register.getStatus());
    }

    // ── Endpoints outside the limiter ────────────────────────────────

    @Test
    void otherEndpoints_shouldPassThrough_withoutLimit() throws Exception {
        // GIVEN
        // Only the three authentication endpoints are throttled. A limit
        // applied to the whole API would throttle ordinary use of the app.

        // WHEN
        for (int i = 1; i <= 30; i++) {
            assertEquals(200, send("GET", "/api/projects", "203.0.113.7").getStatus(), "call " + i);
        }

        // THEN
        verify(filterChain, times(30)).doFilter(any(), any());
        verify(auditService, never()).logLoginFailure(anyString());
    }

    @Test
    void login_shouldNotBeLimited_whenMethodIsNotPost() throws Exception {
        // GIVEN
        // The path alone is not the trigger: the filter matches on method and
        // path together, so a GET on the same URI is not throttled.

        // WHEN
        for (int i = 1; i <= 10; i++) {
            send("GET", "/api/auth/login", "203.0.113.7");
        }

        // THEN
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    // ── IP resolution ────────────────────────────────────────────────

    @Test
    void ip_shouldComeFromForwardedHeader_whenPresent() throws Exception {
        // GIVEN
        // Behind Nginx every request carries the proxy's address in
        // getRemoteAddr. Counting on that would give every client on the
        // internet a single shared bucket, and one attacker would lock
        // everyone out.
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("10.0.0.1");
            request.addHeader("X-Forwarded-For", "203.0.113.7");
            rateLimitFilter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        // WHEN
        // Same proxy address, different client behind it.
        MockHttpServletRequest other = new MockHttpServletRequest("POST", "/api/auth/login");
        other.setRemoteAddr("10.0.0.1");
        other.addHeader("X-Forwarded-For", "198.51.100.2");
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilter(other, response, filterChain);

        // THEN
        assertEquals(200, response.getStatus());
    }

    @Test
    void ip_shouldKeepFirstAddress_whenForwardedHeaderIsAChain() throws Exception {
        // GIVEN
        // X-Forwarded-For accumulates one address per proxy traversed, the
        // client being first. Taking the last would bucket every client behind
        // the nearest proxy together.
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.addHeader("X-Forwarded-For", "203.0.113.7, 198.51.100.2");
            rateLimitFilter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        // WHEN
        // Same client first, different intermediate proxy: same bucket.
        MockHttpServletRequest sameClient = new MockHttpServletRequest("POST", "/api/auth/login");
        sameClient.addHeader("X-Forwarded-For", "203.0.113.7, 192.0.2.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilter(sameClient, response, filterChain);

        // THEN
        assertEquals(429, response.getStatus());
    }

    @Test
    void ip_shouldFallBackToRemoteAddr_whenForwardedHeaderIsEmpty() throws Exception {
        // GIVEN
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
            request.setRemoteAddr("203.0.113.7");
            request.addHeader("X-Forwarded-For", "");
            rateLimitFilter.doFilter(request, new MockHttpServletResponse(), filterChain);
        }

        // WHEN
        MockHttpServletRequest sixth = new MockHttpServletRequest("POST", "/api/auth/login");
        sixth.setRemoteAddr("203.0.113.7");
        sixth.addHeader("X-Forwarded-For", "");
        MockHttpServletResponse response = new MockHttpServletResponse();
        rateLimitFilter.doFilter(sixth, response, filterChain);

        // THEN
        assertEquals(429, response.getStatus());
    }

    // ── Rejection response ───────────────────────────────────────────

    @Test
    void rejection_shouldWriteJsonBody_andAuditTheViolation() throws Exception {
        // GIVEN
        for (int i = 1; i <= 5; i++) {
            send("POST", "/api/auth/login", "203.0.113.7");
        }

        // WHEN
        MockHttpServletResponse rejected = send("POST", "/api/auth/login", "203.0.113.7");

        // THEN
        assertEquals(429, rejected.getStatus());
        // setContentType and setCharacterEncoding are recomposed into a single
        // header by the servlet response, so the charset is asserted here rather
        // than separately.
        assertEquals("application/json;charset=UTF-8", rejected.getContentType());
        assertEquals(
                "{\"status\":429,\"message\":\"Too many requests. Please try again later.\"}",
                rejected.getContentAsString());
        // The IP is what makes the audit line actionable: without it there is
        // no way to tell a distributed attempt from a single noisy client.
        verify(auditService).logLoginFailure("203.0.113.7");
    }

    @Test
    void rejection_shouldNotAudit_whileWithinTheLimit() throws Exception {
        // WHEN
        for (int i = 1; i <= 5; i++) {
            send("POST", "/api/auth/login", "203.0.113.7");
        }

        // THEN
        // A failed login is audited by the service, not here. Logging every
        // allowed attempt from this filter would double every entry.
        verify(auditService, never()).logLoginFailure(anyString());
    }
}
