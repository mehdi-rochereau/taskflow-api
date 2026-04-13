package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.user.UserService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentification", description = "Création de compte et connexion")
@RestController
@RequestMapping(value = "/api/auth", produces = "application/json")
public class AuthController {

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
     * @return {@code 201 Created} with the JWT token and user details,
     *         or {@code 400 Bad Request} if validation fails or the username/email is already taken
     */
    @Operation(
            summary = "Créer un compte utilisateur",
            description = "Crée un nouveau compte et retourne un token JWT valide 24h",
            responses = {
                    @ApiResponse(
                            responseCode = "201",
                            description = "Compte créé avec succès",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Données invalides ou username/email déjà pris",
                            content = @Content
                    )
            }
    )
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * <p>Accepts either a username or an email address as identifier.
     * Delegates credential verification to Spring Security's
     * {@link org.springframework.security.authentication.AuthenticationManager}.</p>
     *
     * @param request the login data — identifier (username or email) and password
     * @return {@code 200 OK} with the JWT token and user details,
     *         {@code 400 Bad Request} if required fields are missing or blank,
     *         or {@code 401 Unauthorized} if the credentials are invalid
     */
    @Operation(
            summary = "Se connecter",
            description = "Authentifie un utilisateur et retourne un token JWT valide 24h",
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Connexion réussie",
                            content = @Content(schema = @Schema(implementation = AuthResponse.class))
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Champs manquants ou invalides",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Identifiants incorrects",
                            content = @Content
                    )
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = userService.login(request);
        return ResponseEntity.ok(response);
    }
}