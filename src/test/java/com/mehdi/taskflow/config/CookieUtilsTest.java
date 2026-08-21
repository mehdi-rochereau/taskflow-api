package com.mehdi.taskflow.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class CookieUtilsTest {

    // No mock and no injection: the class is final with static methods only.
    // MockHttpServletResponse collects the headers actually written, which is
    // the whole observable behaviour of this class.
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        response = new MockHttpServletResponse();
    }

    /** Asserts that the Set-Cookie header carries every expected attribute. */
    private void assertCookieAttributes(
            String name, String value, String path, String maxAge, boolean secure) {
        // MockHttpServletResponse parses the header and rebuilds it in its own
        // order, adding an Expires it derives from Max-Age. Asserting the whole
        // string would test the framework's reconstruction rather than the code
        // under test, and would break on any spring-test upgrade.
        String header = response.getHeader("Set-Cookie");
        Assertions.assertNotNull(header);
        assertTrue(header.startsWith(name + "=" + value + ";"), "name and value: " + header);
        assertTrue(header.contains("Path=" + path), "path: " + header);
        assertTrue(header.contains("Max-Age=" + maxAge), "max age: " + header);
        assertTrue(header.contains("HttpOnly"), "HttpOnly: " + header);
        assertTrue(header.contains("SameSite=Strict"), "SameSite: " + header);
        assertEquals(secure, header.contains("Secure"), "Secure flag: " + header);
    }

    // ── addCookie ────────────────────────────────────────────────────

    @Test
    void addCookie_shouldWriteAllSecurityAttributes_whenSecureIsTrue() {
        // WHEN
        CookieUtils.addCookie(response, "jwt", "token-value", "/api", 900, true);

        // THEN
        assertCookieAttributes("jwt", "token-value", "/api", "900", true);
    }

    @Test
    void addCookie_shouldOmitSecureFlag_whenSecureIsFalse() {
        // GIVEN
        // The development profile runs over plain HTTP, where a Secure cookie
        // would never be sent back by the browser at all.

        // WHEN
        CookieUtils.addCookie(response, "jwt", "token-value", "/api", 900, false);

        // THEN
        assertCookieAttributes("jwt", "token-value", "/api", "900", false);
    }

    @Test
    void addCookie_shouldScopeToTheGivenPath() {
        // GIVEN
        // The refresh token is scoped to /api/auth so that it is not sent on
        // every ordinary API call, only where it can be exchanged.

        // WHEN
        CookieUtils.addCookie(response, "refreshToken", "refresh-value", "/api/auth", 604800, true);

        // THEN
        assertCookieAttributes("refreshToken", "refresh-value", "/api/auth", "604800", true);
    }

    // ── clearCookie ──────────────────────────────────────────────────

    @Test
    void clearCookie_shouldWriteEmptyValueAndZeroMaxAge_whenSecureIsTrue() {
        // WHEN
        CookieUtils.clearCookie(response, "jwt", "/api", true);

        // THEN
        // Max-Age=0 is what tells the browser to drop the cookie immediately;
        // the empty value alone would leave it in place until expiry.
        assertCookieAttributes("jwt", "", "/api", "0", true);
    }

    @Test
    void clearCookie_shouldOmitSecureFlag_whenSecureIsFalse() {
        // WHEN
        CookieUtils.clearCookie(response, "jwt", "/api", false);

        // THEN
        assertCookieAttributes("jwt", "", "/api", "0", false);
    }

    @Test
    void clearCookie_shouldMatchThePathUsedWhenSetting() {
        // GIVEN
        // A clear on a different path leaves the original cookie untouched:
        // browsers match cookies on name and path together, so logout would
        // silently fail to revoke the refresh token.

        // WHEN
        CookieUtils.clearCookie(response, "refreshToken", "/api/auth", true);

        // THEN
        assertCookieAttributes("refreshToken", "", "/api/auth", "0", true);
    }

    // ── class contract ───────────────────────────────────────────────

    @Test
    void constructor_shouldBePrivate_toPreventInstantiation() throws Exception {
        // GIVEN
        // The private constructor is never called by production code, so it
        // stays uncovered and the class never reaches full coverage. Asserting
        // the contract is more useful than instantiating it for the sake of a
        // percentage.
        Constructor<CookieUtils> constructor = CookieUtils.class.getDeclaredConstructor();

        // THEN
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        assertTrue(Modifier.isFinal(CookieUtils.class.getModifiers()));
    }
}
