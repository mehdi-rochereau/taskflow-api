package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.user.UserService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling user authentication operations.
 *
 * <p>Exposes public endpoints for account registration, login,
 * token refresh and logout.</p>
 *
 * <p>All responses are produced in {@code application/json} format.
 * Successful authentication returns an {@link AuthResponse} containing
 * a signed JWT token valid for 24 hours.</p>
 *
 * @see UserService
 */
@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
public class AuthController implements AuthControllerApi {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    /**
     * Constructs a new {@code AuthController} with its required dependency.
     *
     * @param userService service handling registration and authentication logic
     * @param refreshTokenService service handling refresh token operations
     */
    public AuthController(UserService userService, RefreshTokenService refreshTokenService) {
        this.userService = userService;
        this.refreshTokenService = refreshTokenService;
    }

    /**
     * Registers a new user account.
     *
     * <p>Validates the request body, creates the account with a BCrypt-encoded password,
     * and returns a JWT token upon successful registration.</p>
     *
     * @param request the registration data — username, email and password
     * @param response the HTTP response used to write the JWT HttpOnly cookie
     * @return {@code 201 Created} with the JWT token and user details,
     * or {@code 400 Bad Request} if validation fails or the username/email is already taken
     */
    @Override
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request,
                                                 HttpServletResponse response) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(userService.register(request, response));
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Accepts either a username or an email address as identifier.
     * Delegates credential verification to Spring Security's
     * {@link org.springframework.security.authentication.AuthenticationManager}.</p>
     *
     * @param request the login data — identifier (username or email) and password
     * @param response the HTTP response used to write the JWT HttpOnly cookie
     * @return {@code 200 OK} with the JWT token and user details,
     * {@code 400 Bad Request} if required fields are missing or blank,
     * or {@code 401 Unauthorized} if the credentials are invalid
     */
    @Override
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        return ResponseEntity.ok(userService.login(request, response));
    }

    /**
     * Refreshes the JWT access token using a valid refresh token cookie.
     *
     * <p>Validates the {@code refreshToken} HttpOnly cookie, revokes it,
     * generates a new JWT and a new refresh token (rotation strategy),
     * and writes both as HttpOnly cookies in the response.</p>
     *
     * @param request  the HTTP request containing the {@code refreshToken} cookie
     * @param response the HTTP response used to write the new cookies
     * @return {@code 200 OK} with the new JWT token and user details,
     *         or {@code 401 Unauthorized} if the refresh token is missing, revoked or expired
     */
    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(HttpServletRequest request,
                                                HttpServletResponse response) {
        return ResponseEntity.ok(refreshTokenService.refresh(request, response));
    }

    /**
     * Logs out the authenticated user by revoking all refresh tokens.
     *
     * <p>Revokes all active refresh tokens for the current user
     * and clears the {@code jwt} and {@code refreshToken} HttpOnly cookies.</p>
     *
     * @param request  the HTTP request containing the {@code refreshToken} cookie
     * @param response the HTTP response used to clear the cookies
     * @return {@code 204 No Content} on success
     */
    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        refreshTokenService.logout(request, response);
        return ResponseEntity.noContent().build();
    }
}