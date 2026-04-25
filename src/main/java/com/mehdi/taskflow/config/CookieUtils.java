package com.mehdi.taskflow.config;

import jakarta.servlet.http.HttpServletResponse;

/**
 * Utility class for writing and clearing HttpOnly cookies in HTTP responses.
 *
 * <p>Centralizes cookie management to avoid duplication between
 * {@link com.mehdi.taskflow.user.UserService} and
 * {@link com.mehdi.taskflow.auth.RefreshTokenService}.</p>
 *
 * <p>All cookies are configured with {@code HttpOnly} and {@code SameSite=Strict}
 * to prevent XSS token theft and CSRF attacks.
 * The {@code Secure} flag is controlled by the {@code secure} parameter
 * and should be set to {@code true} in production (HTTPS).</p>
 */
public final class CookieUtils {

    private CookieUtils() {}

    /**
     * Writes an HttpOnly cookie in the HTTP response.
     *
     * @param response the HTTP response to write the cookie to
     * @param name     the cookie name
     * @param value    the cookie value
     * @param path     the cookie path — scopes the cookie to specific endpoints
     * @param maxAge   the cookie max age in seconds
     * @param secure   whether to add the {@code Secure} flag (HTTPS only)
     */
    public static void addCookie(HttpServletResponse response,
                                 String name,
                                 String value,
                                 String path,
                                 int maxAge,
                                 boolean secure) {
        String cookieValue = name + "=" + value
                + "; HttpOnly"
                + "; Path=" + path
                + "; Max-Age=" + maxAge
                + "; SameSite=Strict"
                + (secure ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookieValue);
    }

    /**
     * Clears an HttpOnly cookie by setting its {@code Max-Age} to 0.
     *
     * @param response the HTTP response to write the cookie to
     * @param name     the cookie name to clear
     * @param path     the cookie path — must match the path used when setting the cookie
     * @param secure   whether to add the {@code Secure} flag
     */
    public static void clearCookie(HttpServletResponse response,
                                   String name,
                                   String path,
                                   boolean secure) {
        String cookieValue = name + "="
                + "; HttpOnly"
                + "; Path=" + path
                + "; Max-Age=0"
                + "; SameSite=Strict"
                + (secure ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookieValue);
    }
}