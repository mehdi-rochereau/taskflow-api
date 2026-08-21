package com.mehdi.taskflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class AuditServiceTest {

    private AuditService auditService;
    private ListAppender<ILoggingEvent> appender;
    private Logger auditLogger;

    @BeforeEach
    void setUp() {
        auditService = new AuditService();

        // The class under test resolves its logger by name through the static
        // LoggerFactory, so there is nothing to inject. Attaching an appender to
        // that same named logger is the only way to observe what it writes.
        LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
        auditLogger = context.getLogger("AUDIT");

        appender = new ListAppender<>();
        appender.setContext(context);
        appender.start();
        auditLogger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        // The logger is a process-wide singleton: leaving the appender attached
        // would keep collecting events from every later test class in the same
        // JVM, and RequestContextHolder stores its attributes in a thread-local
        // that JUnit's reused threads would carry into unrelated tests.
        auditLogger.detachAppender(appender);
        appender.stop();
        RequestContextHolder.resetRequestAttributes();
    }

    /** Binds a request carrying the given X-Forwarded-For value to the current thread. */
    private void givenRequestWithForwardedFor(String forwardedFor) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");
        if (forwardedFor != null) {
            request.addHeader("X-Forwarded-For", forwardedFor);
        }
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    /** Returns the single event collected, failing if the count is not exactly one. */
    private ILoggingEvent singleEvent() {
        assertEquals(1, appender.list.size(), "exactly one audit event expected");
        return appender.list.getFirst();
    }

    @Test
    void extractIp_shouldReturnUnknown_whenNoRequestContext() {
        // GIVEN
        RequestContextHolder.resetRequestAttributes();

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=unknown"));
    }

    @Test
    void extractIp_shouldUseForwardedHeader_whenPresent() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=203.0.113.7"));
    }

    @Test
    void extractIp_shouldKeepFirstAddress_whenForwardedHeaderIsAChain() {
        // GIVEN
        // X-Forwarded-For accumulates one address per proxy traversed, the client
        // being first. Taking the last would log the proxy's own address.
        givenRequestWithForwardedFor("203.0.113.7, 198.51.100.2, 192.0.2.9");

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=203.0.113.7"));
    }

    @Test
    void extractIp_shouldTrimWhitespace_aroundTheFirstAddress() {
        // GIVEN
        givenRequestWithForwardedFor("  203.0.113.7  , 198.51.100.2");

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=203.0.113.7"));
    }

    @Test
    void extractIp_shouldFallBackToRemoteAddr_whenHeaderIsAbsent() {
        // GIVEN
        givenRequestWithForwardedFor(null);

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=10.0.0.1"));
    }

    @Test
    void extractIp_shouldFallBackToRemoteAddr_whenHeaderIsEmpty() {
        // GIVEN
        givenRequestWithForwardedFor("");

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=10.0.0.1"));
    }

    @Test
    void extractIp_shouldReturnUnknown_whenTheRequestThrows() {
        // GIVEN
        // The catch block exists so that a failure to resolve the IP never
        // prevents the audit entry itself from being written.
        HttpServletRequest failing = mock(HttpServletRequest.class);
        when(failing.getHeader("X-Forwarded-For"))
                .thenThrow(new IllegalStateException("request already recycled"));
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(failing));

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        assertTrue(singleEvent().getFormattedMessage().contains("ip=unknown"));
        assertEquals(Level.INFO, singleEvent().getLevel());
    }

    // ── Logging methods ──────────────────────────────────────────────
    // Each method is checked on three things: the event prefix, which is what a
    // log aggregator filters on; the level, which decides whether the entry
    // surfaces or drowns in the noise; and the parameters, which are what makes
    // the entry usable after the fact. Asserting only that the call does not
    // throw would let a method log the wrong prefix or drop the IP unnoticed.

    @Test
    void logLoginSuccess_shouldLogAtInfo_withUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logLoginSuccess("mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals("[LOGIN_SUCCESS] username=mehdi ip=203.0.113.7", event.getFormattedMessage());
    }

    @Test
    void logLoginFailure_shouldLogAtWarn_withIdentifierAndIp() {
        // GIVEN
        // A failed attempt is logged at WARN, not INFO: repeated failures on the
        // same identifier are what a brute-force attempt looks like in the logs.
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logLoginFailure("mehdi@example.com");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.WARN, event.getLevel());
        assertEquals(
                "[LOGIN_FAILURE] identifier=mehdi@example.com ip=203.0.113.7",
                event.getFormattedMessage());
    }

    @Test
    void logRegisterSuccess_shouldLogAtInfo_withUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logRegisterSuccess("mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "[REGISTER_SUCCESS] username=mehdi ip=203.0.113.7", event.getFormattedMessage());
    }

    @Test
    void logProjectDeletion_shouldLogAtInfo_withProjectIdUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logProjectDeletion(42L, "mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "[PROJECT_DELETE] projectId=42 username=mehdi ip=203.0.113.7",
                event.getFormattedMessage());
    }

    @Test
    void logTaskDeletion_shouldLogAtInfo_withTaskIdUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logTaskDeletion(7L, "mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "[TASK_DELETE] taskId=7 username=mehdi ip=203.0.113.7",
                event.getFormattedMessage());
    }

    @Test
    void logUnexpectedError_shouldLogAtError_withTypeAndMessage() {
        // GIVEN
        // No IP here: this one is called from GlobalExceptionHandler, which may
        // run outside a request context. The exception type and message are
        // logged, never the stack trace, which must not reach the client.
        IllegalStateException ex = new IllegalStateException("connection pool exhausted");

        // WHEN
        auditService.logUnexpectedError(ex);

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.ERROR, event.getLevel());
        assertEquals(
                "[UNEXPECTED_ERROR] type=IllegalStateException message=connection pool exhausted",
                event.getFormattedMessage());
    }

    @Test
    void logSanitizationAttempt_shouldLogAtWarn_withFieldIpOriginalAndSanitized() {
        // GIVEN
        // Both the original and the sanitized value are kept: without the
        // original there is no way to tell an XSS attempt from a user who
        // happened to type an angle bracket.
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logSanitizationAttempt("username", "<script>x</script>", "x");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.WARN, event.getLevel());
        assertEquals(
                "[SANITIZATION] field=username ip=203.0.113.7 original=<script>x</script>"
                        + " sanitized=x",
                event.getFormattedMessage());
    }

    @Test
    void logTokenPurge_shouldLogAtInfo_withCount() {
        // GIVEN
        // No IP: the purge is triggered by the scheduler, with no HTTP request
        // behind it.

        // WHEN
        auditService.logTokenPurge(12);

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals("[TOKEN_PURGE] deleted=12", event.getFormattedMessage());
    }

    @Test
    void logPasswordChange_shouldLogAtInfo_withUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logPasswordChange("mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals(
                "[PASSWORD_CHANGE] username=mehdi ip=203.0.113.7", event.getFormattedMessage());
    }

    @Test
    void logProfileUpdate_shouldLogAtInfo_withUsernameAndIp() {
        // GIVEN
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logProfileUpdate("mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.INFO, event.getLevel());
        assertEquals("[PROFILE_UPDATE] username=mehdi ip=203.0.113.7", event.getFormattedMessage());
    }

    @Test
    void logAccountDeletion_shouldLogAtWarn_withUsernameAndIp() {
        // GIVEN
        // WARN rather than INFO because the operation is irreversible: an
        // account deletion must not drown among ordinary login entries.
        givenRequestWithForwardedFor("203.0.113.7");

        // WHEN
        auditService.logAccountDeletion("mehdi");

        // THEN
        ILoggingEvent event = singleEvent();
        assertEquals(Level.WARN, event.getLevel());
        assertEquals("[ACCOUNT_DELETE] username=mehdi ip=203.0.113.7", event.getFormattedMessage());
    }
}
