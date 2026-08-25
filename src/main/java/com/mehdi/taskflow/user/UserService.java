package com.mehdi.taskflow.user;

import com.mehdi.taskflow.auth.RefreshToken;
import com.mehdi.taskflow.auth.RefreshTokenService;
import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.CookieUtils;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SanitizationService;
import com.mehdi.taskflow.exception.PasswordVerificationException;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.ChangePasswordRequest;
import com.mehdi.taskflow.user.dto.DeleteAccountRequest;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import com.mehdi.taskflow.user.dto.UpdateProfileRequest;
import com.mehdi.taskflow.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service carrying both session establishment and account management for users.
 *
 * <p>Session establishment covers {@code register} and {@code login}. Passwords are encoded using
 * BCrypt before persistence. On success a signed JWT is written to the {@code jwt} HttpOnly cookie,
 * alongside a {@code refreshToken} cookie; neither value appears in the response body. Login
 * accepts either a username or an email address as identifier, delegating credential verification
 * to {@link AuthenticationManager}.
 *
 * <p>Account management covers {@code getUserProfile}, {@code changePassword}, {@code
 * updateProfile} and {@code deleteAccount}, all operating on the user resolved from the current
 * security context through {@link SecurityUtils}.
 *
 * <p>These are two responsibilities, not one, and they belong in separate services. The split into
 * a dedicated authentication service was measured and deliberately deferred, and is recorded in the
 * pending items of {@code avancement-api.md}. This Javadoc describes what the class currently does
 * rather than what its name suggests.
 *
 * @see JwtService
 * @see AuthenticationManager
 * @see SecurityUtils
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MessageService messageService;
    private final AuditService auditService;
    private final RefreshTokenService refreshTokenService;
    private final SanitizationService sanitizationService;
    private final SecurityUtils securityUtils;

    @Value("${application.jwt.expiration}")
    private long jwtExpiration;

    @Value("${application.cookie.secure:false}")
    private boolean cookieSecure;

    /**
     * Constructs a new {@code UserService} with its required dependencies.
     *
     * @param userRepository repository for user persistence
     * @param passwordEncoder BCrypt encoder for password hashing
     * @param jwtService service for JWT token generation
     * @param authenticationManager Spring Security authentication manager
     * @param messageService utility component for resolving i18n messages based on the current
     *     request locale
     * @param auditService service for logging security audit events
     * @param refreshTokenService service for refresh token generation and management
     * @param sanitizationService service for sanitizing user-provided text input
     * @param securityUtils utility for resolving the currently authenticated user
     */
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            MessageService messageService,
            AuditService auditService,
            RefreshTokenService refreshTokenService,
            SanitizationService sanitizationService,
            SecurityUtils securityUtils) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.messageService = messageService;
        this.auditService = auditService;
        this.refreshTokenService = refreshTokenService;
        this.sanitizationService = sanitizationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Registers a new user account.
     *
     * <p>A refresh token valid for the configured expiration period is also issued and stored as an
     * HttpOnly cookie named {@code refreshToken}.
     *
     * <p>Validates that the username and email are not already taken, encodes the password with
     * BCrypt, persists the user, and writes a JWT valid for 15 minutes to the {@code jwt} HttpOnly
     * cookie.
     *
     * <p>The username is sanitized before persistence to prevent XSS attacks.
     *
     * @param request registration data containing username, email and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} carrying the user's identity. The JWT is written to the
     *     {@code jwt} cookie, not returned in the body
     * @throws IllegalArgumentException if the username or email is already in use
     */
    @Transactional
    public AuthResponse register(RegisterRequest request, HttpServletResponse response) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException(messageService.get("error.username.taken"));
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(messageService.get("error.email.taken"));
        }

        User user = new User();
        user.setUsername(
                sanitizationService.sanitizeAndLog(
                        request.getUsername(), "username", auditService));
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);

        auditService.logRegisterSuccess(user.getUsername());

        String token = jwtService.generateToken(user);
        CookieUtils.addCookie(
                response,
                "jwt",
                token,
                "/api",
                (int) Duration.ofMillis(jwtExpiration).toSeconds(),
                cookieSecure);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.addRefreshTokenCookie(response, refreshToken.getToken());

        return new AuthResponse(user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user and issues the authentication cookies.
     *
     * <p>This method is annotated with {@code @Transactional} (not read-only) because it generates
     * and persists a refresh token in the database.
     *
     * <p>A refresh token valid for the configured expiration period is also issued and stored as an
     * HttpOnly cookie named {@code refreshToken}.
     *
     * <p>Accepts either a username or an email address as identifier. Delegates credential
     * verification to {@link AuthenticationManager} — if credentials are invalid, a {@code
     * BadCredentialsException} is thrown before any database lookup occurs.
     *
     * @param request login data containing the identifier (username or email) and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} carrying the user's identity. The JWT is written to the
     *     {@code jwt} cookie, not returned in the body
     * @throws org.springframework.security.authentication.BadCredentialsException if the
     *     credentials are invalid
     * @throws IllegalArgumentException if no user matches the provided identifier
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(), request.getPassword()));

        User user =
                userRepository
                        .findByUsername(request.getIdentifier())
                        .or(() -> userRepository.findByEmail(request.getIdentifier()))
                        .orElseThrow(
                                () ->
                                        new IllegalArgumentException(
                                                messageService.get("error.user.not.found")));

        String token = jwtService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(user.getUsername(), user.getEmail());

        CookieUtils.addCookie(
                response,
                "jwt",
                token,
                "/api",
                (int) Duration.ofMillis(jwtExpiration).toSeconds(),
                cookieSecure);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.addRefreshTokenCookie(response, refreshToken.getToken());

        auditService.logLoginSuccess(user.getUsername());

        return authResponse;
    }

    /**
     * Returns the public profile of the currently authenticated user.
     *
     * <p>Resolves the authenticated user from the current {@link
     * org.springframework.security.core.context.SecurityContext} via {@link
     * SecurityUtils#getCurrentUser()} and maps it to a {@link UserResponse} DTO — password and
     * sensitive fields are never exposed.
     *
     * @return a {@link UserResponse} containing the authenticated user's public profile
     * @throws org.springframework.security.access.AccessDeniedException if no authenticated user is
     *     present in the current security context
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public UserResponse getUserProfile() {
        User currentUser = securityUtils.getCurrentUser();
        return new UserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getRole(),
                currentUser.getCreatedAt());
    }

    /**
     * Changes the authenticated user's password.
     *
     * <p>Verifies the current password against the stored BCrypt hash before applying the new
     * password. Rejects the change if the new password is identical to the current one.
     *
     * <p>All active refresh tokens are revoked after a successful password change to invalidate all
     * existing sessions — the user must re-authenticate.
     *
     * @param request the change password data containing the current and new passwords
     * @throws PasswordVerificationException if the current password does not match the stored hash
     * @throws IllegalArgumentException if the new password is identical to the current one
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Verification of an identity, not validation of a value: the submitted
        // password is well formed and simply wrong. Answered 422, unlike the
        // rule below.
        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new PasswordVerificationException(messageService.get("error.password.incorrect"));
        }

        // Stays on IllegalArgumentException and its 400: this constrains the
        // submitted value, like its length and complexity, which bean validation
        // already answers 400 for. It lives here rather than in an annotation
        // only because it needs the stored hash to be evaluated.

        if (passwordEncoder.matches(request.getNewPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException(messageService.get("error.password.same"));
        }

        currentUser.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(currentUser);

        refreshTokenService.revokeAllUserTokens(currentUser);
        auditService.logPasswordChange(currentUser.getUsername());
    }

    /**
     * Updates the authenticated user's profile.
     *
     * <p>Validates that the new username and email are not already taken by another account before
     * applying the changes. The current user's own username and email are excluded from the
     * uniqueness check to allow re-submitting unchanged values.
     *
     * <p>The new username is sanitized before persistence to prevent XSS attacks.
     *
     * <p>When the username changes, a new JWT is issued and written to the {@code jwt} cookie. The
     * token subject carries the username, so the previous one would designate a row that no longer
     * answers to that name, and every subsequent request would be rejected as an invalid token.
     *
     * @param request the updated profile data containing the new username and email
     * @param response the HTTP response used to write the refreshed JWT cookie on a rename
     * @return a {@link UserResponse} containing the updated user's public profile
     * @throws IllegalArgumentException if the new username or email is already taken by another
     *     account
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request, HttpServletResponse response) {
        User currentUser = securityUtils.getCurrentUser();

        String sanitizedUsername =
                sanitizationService.sanitizeAndLog(request.getUsername(), "username", auditService);

        if (!currentUser.getUsername().equals(sanitizedUsername)
                && userRepository.existsByUsername(sanitizedUsername)) {
            throw new IllegalArgumentException(messageService.get("error.username.taken.other"));
        }

        if (!currentUser.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(messageService.get("error.email.taken.other"));
        }

        // Captured before the change: the JWT subject carries the username, so a
        // rename leaves every already-issued token pointing at a row that no
        // longer answers to that name. JwtFilter then fails to load the user and
        // answers 401 on a session that never expired and was never revoked.
        boolean usernameChanged = !currentUser.getUsername().equals(sanitizedUsername);

        currentUser.setUsername(sanitizedUsername);
        currentUser.setEmail(request.getEmail());
        userRepository.save(currentUser);

        // Re-issued only on a rename. Doing it on every update would silently
        // extend the fifteen-minute window each time an email is corrected,
        // which is a session lifetime decision nobody asked for.
        if (usernameChanged) {
            String token = jwtService.generateToken(currentUser);
            CookieUtils.addCookie(
                    response,
                    "jwt",
                    token,
                    "/api",
                    (int) Duration.ofMillis(jwtExpiration).toSeconds(),
                    cookieSecure);
        }

        auditService.logProfileUpdate(currentUser.getUsername());

        return new UserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getRole(),
                currentUser.getCreatedAt());
    }

    /**
     * Permanently deletes the authenticated user's account.
     *
     * <p>Requires password confirmation to prevent accidental or unauthorized deletion. Associated
     * data is removed by the database itself through {@code ON DELETE} clauses declared on the
     * foreign keys, not by JPA cascading: no {@code @OneToMany} association is declared on {@link
     * User}.
     *
     * <p>Deleted along with the account:
     *
     * <ul>
     *   <li>Projects owned by the user, via {@code fk_projects_owner ON DELETE CASCADE}
     *   <li>Tasks belonging to those projects, via {@code fk_tasks_project ON DELETE CASCADE}
     *   <li>Refresh tokens issued to the user, via {@code fk_refresh_tokens_user ON DELETE CASCADE}
     *   <li>Provider links, via {@code fk_user_providers_user ON DELETE CASCADE}
     * </ul>
     *
     * <p>Not deleted: tasks assigned to the user inside a project owned by someone else. Their
     * {@code assignee_id} is set to {@code NULL} via {@code fk_tasks_assignee ON DELETE SET NULL} —
     * a task belongs to its project, not to its assignee.
     *
     * <p>Both HttpOnly cookies ({@code jwt} and {@code refreshToken}) are cleared from the response
     * after successful deletion to invalidate the current session.
     *
     * <p>This operation is irreversible.
     *
     * @param request the deletion confirmation data containing the user's current password
     * @param response the HTTP response used to clear the JWT and refresh token cookies
     * @throws PasswordVerificationException if the provided password does not match the stored
     *     BCrypt hash
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void deleteAccount(DeleteAccountRequest request, HttpServletResponse response) {
        User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            throw new PasswordVerificationException(
                    messageService.get("error.account.deletion.invalid.password"));
        }

        auditService.logAccountDeletion(currentUser.getUsername());
        userRepository.delete(currentUser);

        CookieUtils.clearCookie(response, "jwt", "/api", cookieSecure);
        CookieUtils.clearCookie(response, "refreshToken", "/api/auth", cookieSecure);
    }
}
