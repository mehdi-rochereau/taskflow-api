package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO carrying the data required to change the authenticated user's password.
 *
 * <p>Used as the request body for {@code POST /api/users/me/password}.
 * Both fields are required — the current password is verified before
 * the new password is applied.</p>
 *
 * <p>The new password must meet the application's strength requirements:
 * at least 8 characters, one uppercase letter, one digit and one special character.</p>
 *
 * @see com.mehdi.taskflow.user.UserService#changePassword(ChangePasswordRequest)
 * @see com.mehdi.taskflow.user.UserController
 */
@Schema(
        name = "ChangePasswordRequest",
        description = "Request body for changing the authenticated user's password"
)
public class ChangePasswordRequest {

    /**
     * The user's current password.
     * Verified against the stored BCrypt hash before applying the new password.
     */
    @Schema(
            description = "The user's current password. Used to verify identity before applying the change.",
            example = "OldPassword@2026",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.password.current.required}")
    private String currentPassword;

    /**
     * The new password to apply.
     *
     * <p>Must meet the following strength requirements:</p>
     * <ul>
     *   <li>At least 8 characters</li>
     *   <li>At least one uppercase letter (A-Z)</li>
     *   <li>At least one digit (0-9)</li>
     *   <li>At least one special character</li>
     * </ul>
     *
     * <p>Encoded with BCrypt before persistence — never stored in plain text.</p>
     */
    @Schema(
            description = """
                    The new password to apply. Must contain at least:
                    - 8 characters
                    - 1 uppercase letter (A-Z)
                    - 1 digit (0-9)
                    - 1 special character
                    Stored as BCrypt hash — never in plain text.
                    """,
            example = "NewPassword@2026",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "{validation.password.required}")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[0-9])(?=.*[^a-zA-Z0-9]).{8,}$",
            message = "{validation.password.strength}"
    )
    private String newPassword;

    /**
     * Default constructor required for JSON deserialization.
     */
    public ChangePasswordRequest() {}

    /** @return the user's current password */
    public String getCurrentPassword() { return currentPassword; }

    /** @param currentPassword the user's current password */
    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    /** @return the new password to apply */
    public String getNewPassword() { return newPassword; }

    /** @param newPassword the new password to apply */
    public void setNewPassword(String newPassword) { this.newPassword = newPassword; }
}
