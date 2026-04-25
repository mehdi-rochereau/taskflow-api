package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.config.CookieUtils;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Service handling refresh token lifecycle operations.
 *
 * <p>Refresh tokens are used to obtain new JWT access tokens without
 * requiring the user to re-authenticate. Each token is single-use —
 * upon use it is revoked and a new one is issued (rotation strategy).</p>
 *
 * <p>Refresh tokens are stored in the database and expire after a
 * configurable duration defined by {@code application.refresh-token.expiration-days}.</p>
 *
 * <p>Public API:</p>
 * <ul>
 *   <li>{@link #generateRefreshToken(User)} — called by {@link com.mehdi.taskflow.user.UserService}</li>
 *   <li>{@link #addRefreshTokenCookie(HttpServletResponse, String)} — called by {@link com.mehdi.taskflow.user.UserService} and internally by {@link #refresh(HttpServletRequest, HttpServletResponse)}</li>
 *   <li>{@link #refresh(HttpServletRequest, HttpServletResponse)} — called by {@link AuthController}</li>
 *   <li>{@link #logout(HttpServletRequest, HttpServletResponse)} — called by {@link AuthController}</li>
 * </ul>
 *
 * @see RefreshToken
 * @see RefreshTokenRepository
 */
@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final MessageService messageService;
    private final JwtService jwtService;

    @Value("${application.refresh-token.expiration-days:7}")
    private int expirationDays;

    @Value("${application.cookie.secure:false}")
    private boolean cookieSecure;

    @Value("${application.jwt.expiration}")
    private long jwtExpiration;

    /**
     * Constructs a new {@code RefreshTokenService} with its required dependencies.
     *
     * @param refreshTokenRepository repository for refresh token persistence
     * @param messageService         utility component for resolving i18n messages
     * @param jwtService             service for JWT token generation
     */
    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               MessageService messageService,
                               JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.messageService = messageService;
        this.jwtService = jwtService;
    }

    /**
     * Generates a new refresh token for the given user and stores it in the database.
     *
     * @param user the user to generate a refresh token for
     * @return the persisted {@link RefreshToken}
     */
    @Transactional
    public RefreshToken generateRefreshToken(User user) {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setToken(UUID.randomUUID().toString());
        refreshToken.setUser(user);
        refreshToken.setExpiresAt(LocalDateTime.now().plusDays(expirationDays));
        refreshToken.setRevoked(false);
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Refreshes the JWT access token using the refresh token from the HttpOnly cookie.
     *
     * <p>Validates the refresh token, revokes it, generates a new JWT and
     * a new refresh token (rotation strategy), and writes both as HttpOnly cookies.</p>
     *
     * @param request  the HTTP request containing the {@code refreshToken} cookie
     * @param response the HTTP response used to write the new cookies
     * @return an {@link AuthResponse} containing the new JWT token and user details
     * @throws IllegalArgumentException  if the refresh token cookie is missing
     * @throws ResourceNotFoundException if the refresh token does not exist
     * @throws IllegalArgumentException  if the refresh token is revoked or expired
     */
    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String token = extractRefreshTokenFromCookie(request);
        if (token == null) {
            throw new IllegalArgumentException(
                    messageService.get("error.refresh.token.not.found"));
        }

        RefreshToken refreshToken = validateRefreshToken(token);
        refreshToken.setRevoked(true);
        refreshTokenRepository.save(refreshToken);

        User user = refreshToken.getUser();
        String newJwt = jwtService.generateToken(user);
        RefreshToken newRefreshToken = generateRefreshToken(user);

        CookieUtils.addCookie(response, "jwt", newJwt, "/api", (int) (jwtExpiration / 1000), cookieSecure);
        addRefreshTokenCookie(response, newRefreshToken.getToken());

        return new AuthResponse(newJwt, user.getUsername(), user.getEmail());
    }

    /**
     * Logs out the authenticated user by revoking all active refresh tokens
     * and clearing the {@code jwt} and {@code refreshToken} cookies.
     *
     * @param request  the HTTP request containing the {@code refreshToken} cookie
     * @param response the HTTP response used to clear the cookies
     */
    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        String token = extractRefreshTokenFromCookie(request);
        if (token != null) {
            refreshTokenRepository.findByToken(token).ifPresent(rt ->
                    revokeAllUserTokens(rt.getUser())
            );
        }
        CookieUtils.clearCookie(response, "jwt", "/api", cookieSecure);
        CookieUtils.clearCookie(response, "refreshToken", "/api/auth", cookieSecure);
    }

    /**
     * Writes the refresh token as an HttpOnly cookie in the HTTP response.
     *
     * <p>The cookie is configured with the following security attributes:</p>
     * <ul>
     *   <li>{@code HttpOnly} — inaccessible to JavaScript, prevents XSS token theft</li>
     *   <li>{@code Secure} — must be set to {@code true} in production (HTTPS)</li>
     *   <li>{@code SameSite=Strict} — prevents CSRF attacks</li>
     *   <li>{@code Path=/api/auth} — scoped to auth endpoints only</li>
     *   <li>{@code Max-Age} — matches refresh token expiration in days</li>
     * </ul>
     *
     * @param response     the HTTP response to write the cookie to
     * @param refreshToken the refresh token string to store in the cookie
     */
    public void addRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        CookieUtils.addCookie(response, "refreshToken", refreshToken, "/api/auth",
                expirationDays * 24 * 60 * 60, cookieSecure);
    }

    /**
     * Validates a refresh token string and returns the associated {@link RefreshToken}.
     *
     * <p>A token is considered valid if it exists in the database,
     * has not been revoked, and has not expired.</p>
     *
     * @param token the refresh token string to validate
     * @return the valid {@link RefreshToken}
     * @throws ResourceNotFoundException if the token does not exist
     * @throws IllegalArgumentException  if the token has been revoked or has expired
     */
    private RefreshToken validateRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException(
                        messageService.get("error.refresh.token.not.found")));

        if (refreshToken.isRevoked()) {
            throw new IllegalArgumentException(
                    messageService.get("error.refresh.token.revoked"));
        }

        if (refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(
                    messageService.get("error.refresh.token.expired"));
        }

        return refreshToken;
    }

    /**
     * Revokes all active refresh tokens for the given user.
     * Used during logout to invalidate all sessions.
     *
     * @param user the user whose tokens should be revoked
     */
    private void revokeAllUserTokens(User user) {
        refreshTokenRepository.revokeAllByUser(user);
    }

    /**
     * Extracts the refresh token string from the {@code refreshToken} HttpOnly cookie.
     *
     * @param request the HTTP request
     * @return the refresh token string, or {@code null} if the cookie is absent
     */
    private String extractRefreshTokenFromCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        for (Cookie cookie : request.getCookies()) {
            if ("refreshToken".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}