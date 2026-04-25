package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.user.UserService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
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
 * <p>Exposes public endpoints for account registration and login.
 * No JWT token is required to access these endpoints.</p>
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

    /**
     * Constructs a new {@code AuthController} with its required dependency.
     *
     * @param userService service handling registration and authentication logic
     */
    public AuthController(UserService userService) {
        this.userService = userService;
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
}