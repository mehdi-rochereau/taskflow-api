package com.mehdi.taskflow.user;

import com.mehdi.taskflow.auth.RefreshToken;
import com.mehdi.taskflow.auth.RefreshTokenService;
import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.CookieUtils;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SanitizationService;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.dto.*;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user authentication operations.
 *
 * <p>Provides user registration and login functionality.
 * Passwords are encoded using BCrypt before persistence.
 * On successful authentication a signed JWT is written to the {@code jwt}
 * HttpOnly cookie, alongside a {@code refreshToken} cookie; neither value
 * appears in the response body.</p>
 *
 * <p>Login accepts either a username or an email address as identifier,
 * delegating credential verification to {@link AuthenticationManager}.</p>
 *
 * @see JwtService
 * @see AuthenticationManager
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
     * @param userRepository        repository for user persistence
     * @param passwordEncoder       BCrypt encoder for password hashing
     * @param jwtService            service for JWT token generation
     * @param authenticationManager Spring Security authentication manager
     * @param messageService        utility component for resolving i18n messages based on the current request locale
     * @param auditService          service for logging security audit events
     * @param refreshTokenService   service for refresh token generation and management
     * @param sanitizationService   service for sanitizing user-provided text input
     * @param securityUtils         utility for resolving the currently authenticated user
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
     * <p>A refresh token valid for the configured expiration period is also issued and stored
     * as an HttpOnly cookie named {@code refreshToken}.</p>
     *
     * <p>Validates that the username and email are not already taken,
     * encodes the password with BCrypt, persists the user, and writes a JWT
     * valid for 15 minutes to the {@code jwt} HttpOnly cookie.</p>
     *
     * <p>The username is sanitized before persistence to prevent XSS attacks.</p>
     *
     * @param request  registration data containing username, email and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} carrying the user's identity. The JWT is
     *         written to the {@code jwt} cookie, not returned in the body
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
        user.setUsername(sanitizationService.sanitizeAndLog(request.getUsername(), "username", auditService));
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);

        auditService.logRegisterSuccess(user.getUsername());

        String token = jwtService.generateToken(user);
        CookieUtils.addCookie(response, "jwt", token, "/api", (int) (jwtExpiration / 1000), cookieSecure);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.addRefreshTokenCookie(response, refreshToken.getToken());

        return new AuthResponse(user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user and issues the authentication cookies.
     *
     * <p>This method is annotated with {@code @Transactional} (not read-only)
     * because it generates and persists a refresh token in the database.</p>
     *
     * <p>A refresh token valid for the configured expiration period is also issued and stored
     * as an HttpOnly cookie named {@code refreshToken}.</p>
     *
     * <p>Accepts either a username or an email address as identifier.
     * Delegates credential verification to {@link AuthenticationManager} —
     * if credentials are invalid, a {@code BadCredentialsException} is thrown
     * before any database lookup occurs.</p>
     *
     * @param request  login data containing the identifier (username or email) and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} carrying the user's identity. The JWT is
     *         written to the {@code jwt} cookie, not returned in the body
     * @throws org.springframework.security.authentication.BadCredentialsException if the credentials are invalid
     * @throws IllegalArgumentException                                            if no user matches the provided identifier
     */
    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getIdentifier(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByUsername(request.getIdentifier())
                .or(() -> userRepository.findByEmail(request.getIdentifier()))
                .orElseThrow(() -> new IllegalArgumentException(
                        messageService.get("error.user.not.found")));

        String token = jwtService.generateToken(user);
        AuthResponse authResponse = new AuthResponse(user.getUsername(), user.getEmail());

        CookieUtils.addCookie(response, "jwt", token, "/api", (int) (jwtExpiration / 1000), cookieSecure);

        RefreshToken refreshToken = refreshTokenService.generateRefreshToken(user);
        refreshTokenService.addRefreshTokenCookie(response, refreshToken.getToken());

        auditService.logLoginSuccess(user.getUsername());

        return authResponse;
    }


    /**
     * Returns the public profile of the currently authenticated user.
     *
     * <p>Resolves the authenticated user from the current
     * {@link org.springframework.security.core.context.SecurityContext}
     * via {@link SecurityUtils#getCurrentUser()} and maps it to a
     * {@link UserResponse} DTO — password and sensitive fields are never exposed.</p>
     *
     * @return a {@link UserResponse} containing the authenticated user's public profile
     * @throws org.springframework.security.access.AccessDeniedException if no authenticated user
     *                                                                   is present in the current security context
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
                currentUser.getCreatedAt()
        );
    }

    /**
     * Changes the authenticated user's password.
     *
     * <p>Verifies the current password against the stored BCrypt hash before
     * applying the new password. Rejects the change if the new password is
     * identical to the current one.</p>
     *
     * <p>All active refresh tokens are revoked after a successful password change
     * to invalidate all existing sessions — the user must re-authenticate.</p>
     *
     * @param request the change password data containing the current and new passwords
     * @throws IllegalArgumentException if the current password is incorrect
     *                                  or if the new password is identical to the current one
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException(messageService.get("error.password.incorrect"));
        }

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
     * <p>Validates that the new username and email are not already taken by another account
     * before applying the changes. The current user's own username and email are excluded
     * from the uniqueness check to allow re-submitting unchanged values.</p>
     *
     * <p>The new username is sanitized before persistence to prevent XSS attacks.</p>
     *
     * @param request the updated profile data containing the new username and email
     * @return a {@link UserResponse} containing the updated user's public profile
     * @throws IllegalArgumentException if the new username or email is already taken
     *                                  by another account
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        String sanitizedUsername = sanitizationService.sanitizeAndLog(
                request.getUsername(), "username", auditService);

        if (!currentUser.getUsername().equals(sanitizedUsername)
                && userRepository.existsByUsername(sanitizedUsername)) {
            throw new IllegalArgumentException(messageService.get("error.username.taken.other"));
        }

        if (!currentUser.getEmail().equals(request.getEmail())
                && userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException(messageService.get("error.email.taken.other"));
        }

        currentUser.setUsername(sanitizedUsername);
        currentUser.setEmail(request.getEmail());
        userRepository.save(currentUser);

        auditService.logProfileUpdate(currentUser.getUsername());

        return new UserResponse(
                currentUser.getId(),
                currentUser.getUsername(),
                currentUser.getEmail(),
                currentUser.getRole(),
                currentUser.getCreatedAt()
        );
    }

    /**
     * Permanently deletes the authenticated user's account.
     *
     * <p>Requires password confirmation to prevent accidental or unauthorized
     * deletion. Associated data is removed by the database itself through
     * {@code ON DELETE} clauses declared on the foreign keys, not by JPA
     * cascading: no {@code @OneToMany} association is declared on {@link User}.</p>
     *
     * <p>Deleted along with the account:</p>
     * <ul>
     *   <li>Projects owned by the user, via {@code fk_projects_owner ON DELETE CASCADE}</li>
     *   <li>Tasks belonging to those projects, via {@code fk_tasks_project ON DELETE CASCADE}</li>
     *   <li>Refresh tokens issued to the user, via {@code fk_refresh_tokens_user ON DELETE CASCADE}</li>
     *   <li>Provider links, via {@code fk_user_providers_user ON DELETE CASCADE}</li>
     * </ul>
     *
     * <p>Not deleted: tasks assigned to the user inside a project owned by
     * someone else. Their {@code assignee_id} is set to {@code NULL} via
     * {@code fk_tasks_assignee ON DELETE SET NULL} — a task belongs to its
     * project, not to its assignee.</p>
     *
     * <p>Both HttpOnly cookies ({@code jwt} and {@code refreshToken}) are cleared
     * from the response after successful deletion to invalidate the current session.</p>
     *
     * <p>This operation is irreversible.</p>
     *
     * @param request  the deletion confirmation data containing the user's current password
     * @param response the HTTP response used to clear the JWT and refresh token cookies
     * @throws IllegalArgumentException if the provided password does not match
     *                                  the stored BCrypt hash
     */
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public void deleteAccount(DeleteAccountRequest request, HttpServletResponse response) {
        User currentUser = securityUtils.getCurrentUser();

        if (!passwordEncoder.matches(request.getPassword(), currentUser.getPassword())) {
            throw new IllegalArgumentException(
                    messageService.get("error.account.deletion.invalid.password"));
        }

        auditService.logAccountDeletion(currentUser.getUsername());
        userRepository.delete(currentUser);

        CookieUtils.clearCookie(response, "jwt", "/api", cookieSecure);
        CookieUtils.clearCookie(response, "refreshToken", "/api/auth", cookieSecure);
    }
}
