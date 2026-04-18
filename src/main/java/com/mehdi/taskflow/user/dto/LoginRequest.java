package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO carrying user login credentials.
 *
 * <p>Used as the request body for {@code POST /api/auth/login}.
 * The {@code identifier} field accepts either a username or an email address,
 * supporting dual-identifier authentication.</p>
 *
 * @see com.mehdi.taskflow.auth.AuthController#login(LoginRequest)
 * @see com.mehdi.taskflow.user.UserService#login(LoginRequest)
 */
@Schema(
        name = "LoginRequest",
        description = "Request body for authenticating an existing user"
)
public class LoginRequest {

    /**
     * Username or email address used to identify the user.
     * Must not be blank.
     */
    @Schema(
            description = "Username or email address. Both are accepted as valid identifiers.",
            example = "mehdi",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.identifier.required}")
    private String identifier;

    /**
     * Plain-text password to verify against the stored BCrypt hash.
     * Must not be blank.
     */
    @Schema(
            description = "Plain-text password to verify against the stored BCrypt hash.",
            example = "password123",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.password.required}")
    private String password;

    /**
     * Default constructor required for JSON deserialization.
     */
    public LoginRequest() {}

    /** @return the username or email identifier */
    public String getIdentifier() { return identifier; }

    /** @param identifier the username or email identifier */
    public void setIdentifier(String identifier) { this.identifier = identifier; }

    /** @return the plain-text password */
    public String getPassword() { return password; }

    /** @param password the plain-text password */
    public void setPassword(String password) { this.password = password; }
}