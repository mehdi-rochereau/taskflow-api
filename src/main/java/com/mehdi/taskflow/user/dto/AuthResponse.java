package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO returned after successful authentication (register or login).
 *
 * <p>Contains the signed JWT token and basic user information.
 * The token is valid for 24 hours and must be passed as a
 * {@code Authorization: Bearer <token>} header on all protected endpoints.</p>
 *
 * @see com.mehdi.taskflow.auth.AuthController
 * @see com.mehdi.taskflow.user.UserService
 */
@Schema(
        name = "AuthResponse",
        description = "Response returned after successful registration or login"
)
public class AuthResponse {

    @Schema(
            description = "Signed JWT token valid for 24 hours. Pass as Authorization: Bearer <token> on all protected endpoints.",
            example = "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJtZWhkaSIsImlhdCI6MTcx..."
    )
    private String token;

    @Schema(
            description = "Username of the authenticated user.",
            example = "mehdi"
    )
    private String username;

    @Schema(
            description = "Email address of the authenticated user.",
            example = "mehdi@example.com"
    )
    private String email;

    /**
     * Default constructor required for JSON deserialization.
     */
    public AuthResponse() {}

    /**
     * Constructs a fully populated authentication response.
     *
     * @param token    the signed JWT token
     * @param username the authenticated user's username
     * @param email    the authenticated user's email address
     */
    public AuthResponse(String token, String username, String email) {
        this.token = token;
        this.username = username;
        this.email = email;
    }

    /** @return the signed JWT token */
    public String getToken() { return token; }

    /** @param token the signed JWT token */
    public void setToken(String token) { this.token = token; }

    /** @return the authenticated user's username */
    public String getUsername() { return username; }

    /** @param username the authenticated user's username */
    public void setUsername(String username) { this.username = username; }

    /** @return the authenticated user's email address */
    public String getEmail() { return email; }

    /** @param email the authenticated user's email address */
    public void setEmail(String email) { this.email = email; }
}