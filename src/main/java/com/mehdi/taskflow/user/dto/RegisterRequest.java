package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO carrying user registration data.
 *
 * <p>Used as the request body for {@code POST /api/auth/register}.
 * All fields are validated before processing — see constraint annotations for details.</p>
 *
 * @see com.mehdi.taskflow.auth.AuthController#register(RegisterRequest)
 * @see com.mehdi.taskflow.user.UserService#register(RegisterRequest)
 */
@Schema(
        name = "RegisterRequest",
        description = "Request body for creating a new user account"
)
public class RegisterRequest {

    /**
     * Unique username for the new account.
     * Must be between 3 and 50 characters and must not be blank.
     */
    @Schema(
            description = "Unique username for the new account. Must be between 3 and 50 characters.",
            example = "mehdi",
            minLength = 3,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 50, message = "{validation.username.size}")
    private String username;

    /**
     * Email address for the new account.
     * Must be a valid email format and must not be blank.
     */
    @Schema(
            description = "Valid email address for the new account. Must be unique.",
            example = "mehdi@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Email(message = "{validation.email.invalid}")
    @NotBlank(message = "{validation.email.required}")
    private String email;

    /**
     * Plain-text password for the new account.
     * Must be at least 8 characters. Encoded with BCrypt before persistence.
     */
    @Schema(
            description = "Plain-text password. Minimum 8 characters. Stored as BCrypt hash.",
            example = "password123",
            minLength = 8,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.password.required}")
    @Size(min = 8, message = "{validation.password.size}")
    private String password;

    /**
     * Default constructor required for JSON deserialization.
     */
    public RegisterRequest() {}

    /** @return the requested username */
    public String getUsername() { return username; }

    /** @param username the requested username */
    public void setUsername(String username) { this.username = username; }

    /** @return the email address */
    public String getEmail() { return email; }

    /** @param email the email address */
    public void setEmail(String email) { this.email = email; }

    /** @return the plain-text password */
    public String getPassword() { return password; }

    /** @param password the plain-text password */
    public void setPassword(String password) { this.password = password; }
}