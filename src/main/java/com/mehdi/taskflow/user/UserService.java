package com.mehdi.taskflow.user;

import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
 * A signed JWT token is returned upon successful authentication.</p>
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
     * @param messageService utility component for resolving i18n messages based on the current request locale
     * @param auditService   service for logging security audit events
     */
    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       AuthenticationManager authenticationManager,
                       MessageService messageService,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.messageService = messageService;
        this.auditService = auditService;
    }

    /**
     * Registers a new user account.
     *
     * <p>Validates that the username and email are not already taken,
     * encodes the password with BCrypt, persists the user,
     * and returns a JWT token valid for 24 hours.</p>
     *
     * @param request registration data containing username, email and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} containing the JWT token and user details
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
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole("ROLE_USER");

        userRepository.save(user);
        auditService.logRegisterSuccess(user.getUsername());

        String token = jwtService.generateToken(user);
        addJwtCookie(response, token);
        return new AuthResponse(token, user.getUsername(), user.getEmail());
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Accepts either a username or an email address as identifier.
     * Delegates credential verification to {@link AuthenticationManager} —
     * if credentials are invalid, a {@code BadCredentialsException} is thrown
     * before any database lookup occurs.</p>
     *
     * @param request login data containing the identifier (username or email) and password
     * @param response HTTP response used to write the JWT HttpOnly cookie
     * @return an {@link AuthResponse} containing the JWT token and user details
     * @throws org.springframework.security.authentication.BadCredentialsException if the credentials are invalid
     * @throws IllegalArgumentException                                            if no user matches the provided identifier
     */
    @Transactional(readOnly = true)
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
        AuthResponse authResponse = new AuthResponse(token, user.getUsername(), user.getEmail());
        addJwtCookie(response, token);
        auditService.logLoginSuccess(user.getUsername());
        return authResponse;
    }

    /**
     * Writes the JWT token as an HttpOnly cookie in the HTTP response.
     *
     * <p>The cookie is configured with the following security attributes:</p>
     * <ul>
     *   <li>{@code HttpOnly} — inaccessible to JavaScript, prevents XSS token theft</li>
     *   <li>{@code Secure} — must be set to {@code true} in production (HTTPS)</li>
     *   <li>{@code SameSite=Strict} — prevents CSRF attacks by blocking cross-site requests</li>
     *   <li>{@code Path=/api} — scoped to API endpoints only</li>
     *   <li>{@code Max-Age} — matches JWT expiration configured via {@code application.jwt.expiration}</li>
     * </ul>
     *
     * <p>Note: {@code SameSite} is not natively supported by the Jakarta Cookie API
     * and is therefore added manually via the {@code Set-Cookie} response header.</p>
     *
     * @param response the HTTP response to write the cookie to
     * @param token    the signed JWT token to store in the cookie
     */
    private void addJwtCookie(HttpServletResponse response, String token) {
        int maxAge = (int) (jwtExpiration / 1000);
        String cookieValue = "jwt=" + token
                + "; HttpOnly"
                + "; Path=/api"
                + "; Max-Age=" + maxAge
                + "; SameSite=Strict"
                + (cookieSecure ? "; Secure" : "");
        response.addHeader("Set-Cookie", cookieValue);
    }
}