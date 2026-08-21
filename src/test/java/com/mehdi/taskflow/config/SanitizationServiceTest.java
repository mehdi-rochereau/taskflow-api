package com.mehdi.taskflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class SanitizationServiceTest {

    // The OWASP policy is a static field of the class under test and is exercised
    // for real: mocking it would verify that a method is called, not that anything
    // is actually stripped. AuditService is mocked because it is a collaborator
    // passed in, and its own behaviour is covered by AuditServiceTest.
    @Mock private AuditService auditService;

    private SanitizationService sanitizationService;

    @BeforeEach
    void setUp() {
        sanitizationService = new SanitizationService();
    }

    // ── sanitize ─────────────────────────────────────────────────────

    @Test
    void sanitize_shouldReturnNull_whenInputIsNull() {
        // WHEN
        String result = sanitizationService.sanitize(null);

        // THEN
        assertNull(result);
    }

    @Test
    void sanitize_shouldReturnEmpty_whenInputIsEmpty() {
        // WHEN
        String result = sanitizationService.sanitize("");

        // THEN
        assertEquals("", result);
    }

    @Test
    void sanitize_shouldLeavePlainTextUnchanged() {
        // WHEN
        String result = sanitizationService.sanitize("Hello World");

        // THEN
        assertEquals("Hello World", result);
    }

    @Test
    void sanitize_shouldDropScriptEntirely_contentIncluded() {
        // GIVEN
        // A script element is dropped together with its content, unlike a
        // formatting tag: stripping the tags alone would leave the payload
        // sitting in the database as plain text.
        String input = "<script>alert('XSS')</script>Hello";

        // WHEN
        String result = sanitizationService.sanitize(input);

        // THEN
        assertEquals("Hello", result);
    }

    @Test
    void sanitize_shouldStripFormattingTags_keepingTheirText() {
        // WHEN
        String result = sanitizationService.sanitize("<b>Bold</b> text");

        // THEN
        assertEquals("Bold text", result);
    }

    @Test
    void sanitize_shouldStripEventHandlerAttributes() {
        // GIVEN
        // The policy allows no attribute at all, so an inline handler cannot
        // survive on any element.
        String input = "<div onclick=\"steal()\">Click</div>";

        // WHEN
        String result = sanitizationService.sanitize(input);

        // THEN
        assertEquals("Click", result);
    }

    @Test
    void sanitize_shouldRestoreApostrophes_afterPolicyEscaping() {
        // GIVEN
        // The OWASP policy escapes apostrophes to &#39;. Without the
        // unescapeHtml4 call, a French username would be stored with the entity
        // visible in it. This is the bug the unescape step was added to fix.
        String input = "L'animal";

        // WHEN
        String result = sanitizationService.sanitize(input);

        // THEN
        assertEquals("L'animal", result);
    }

    @Test
    void sanitize_shouldRestoreAmpersands_afterPolicyEscaping() {
        // WHEN
        String result = sanitizationService.sanitize("Café & croissant");

        // THEN
        assertEquals("Café & croissant", result);
    }

    // ── wasSanitized ─────────────────────────────────────────────────

    @Test
    void wasSanitized_shouldReturnFalse_whenBothAreNull() {
        // WHEN & THEN
        assertFalse(sanitizationService.wasSanitized(null, null));
    }

    @Test
    void wasSanitized_shouldReturnTrue_whenOnlyOriginalIsNull() {
        // WHEN & THEN
        assertTrue(sanitizationService.wasSanitized(null, "Hello"));
    }

    @Test
    void wasSanitized_shouldReturnTrue_whenOnlySanitizedIsNull() {
        // WHEN & THEN
        assertTrue(sanitizationService.wasSanitized("Hello", null));
    }

    @Test
    void wasSanitized_shouldReturnFalse_whenValuesAreEqual() {
        // WHEN & THEN
        assertFalse(sanitizationService.wasSanitized("Hello", "Hello"));
    }

    @Test
    void wasSanitized_shouldReturnTrue_whenValuesDiffer() {
        // WHEN & THEN
        assertTrue(sanitizationService.wasSanitized("<b>Hello</b>", "Hello"));
    }

    // ── sanitizeAndLog ───────────────────────────────────────────────

    @Test
    void sanitizeAndLog_shouldNotLog_whenInputIsPlainText() {
        // WHEN
        String result = sanitizationService.sanitizeAndLog("Hello", "username", auditService);

        // THEN
        assertEquals("Hello", result);
        verify(auditService, never()).logSanitizationAttempt("username", "Hello", "Hello");
    }

    @Test
    void sanitizeAndLog_shouldLogOriginalAndSanitized_whenHtmlWasStripped() {
        // GIVEN
        String input = "<script>alert('XSS')</script>Hello";

        // WHEN
        String result = sanitizationService.sanitizeAndLog(input, "username", auditService);

        // THEN
        assertEquals("Hello", result);
        // The original is passed to the audit log, not just the cleaned value:
        // without it there is no way to tell an attack from a stray angle bracket.
        verify(auditService).logSanitizationAttempt("username", input, "Hello");
    }

    @Test
    void sanitizeAndLog_shouldReturnNullAndNotLog_whenInputIsNull() {
        // WHEN
        String result = sanitizationService.sanitizeAndLog(null, "username", auditService);

        // THEN
        assertNull(result);
        verify(auditService, never()).logSanitizationAttempt(null, null, null);
    }

    @Test
    void sanitizeAndLog_shouldNotLog_whenApostropheIsRestored() {
        // GIVEN
        // The round trip through the policy and back leaves the value identical,
        // so nothing was really stripped and nothing should be logged. Without
        // the unescape step this input would be reported as an XSS attempt.
        String input = "L'animal";

        // WHEN
        String result = sanitizationService.sanitizeAndLog(input, "username", auditService);

        // THEN
        assertEquals("L'animal", result);
        verify(auditService, never()).logSanitizationAttempt("username", input, input);
    }
}
