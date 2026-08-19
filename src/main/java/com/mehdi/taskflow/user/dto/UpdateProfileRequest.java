package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * DTO carrying the data required to update the authenticated user's profile.
 *
 * <p>Used as the request body for {@code PUT /api/users/me}. Both fields are required — partial
 * updates are not supported. Uniqueness of username and email is enforced before persistence.
 *
 * @see com.mehdi.taskflow.user.UserService#updateProfile(UpdateProfileRequest)
 * @see com.mehdi.taskflow.user.UserController
 */
@Schema(
        name = "UpdateProfileRequest",
        description = "Request body for updating the authenticated user's profile")
public class UpdateProfileRequest {

    /**
     * The new username for the account. Must be between 3 and 50 characters, must not be blank, and
     * must be unique across all users.
     */
    @Schema(
            description = "New unique username. Must be between 3 and 50 characters.",
            example = "mehdi_updated",
            minLength = 3,
            maxLength = 50,
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.username.required}")
    @Size(min = 3, max = 50, message = "{validation.username.size}")
    private String username;

    /**
     * The new email address for the account. Must be a valid email format, must not be blank, and
     * must be unique across all users.
     */
    @Schema(
            description = "New unique email address. Must be a valid email format.",
            example = "mehdi.updated@example.com",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.email.required}")
    @Email(message = "{validation.email.invalid}")
    private String email;

    /** Default constructor required for JSON deserialization. */
    public UpdateProfileRequest() {}

    /**
     * @return the new username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the new username
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the new email address
     */
    public String getEmail() {
        return email;
    }

    /**
     * @param email the new email address
     */
    public void setEmail(String email) {
        this.email = email;
    }
}
