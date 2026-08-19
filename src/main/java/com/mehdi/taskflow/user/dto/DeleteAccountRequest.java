package com.mehdi.taskflow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO carrying the data required to permanently delete the authenticated user's account.
 *
 * <p>Used as the request body for {@code DELETE /api/users/me}. The password is required as a
 * confirmation step to prevent accidental or unauthorized account deletion.
 *
 * <p>Account deletion is irreversible — all associated data (projects, tasks, refresh tokens) is
 * permanently removed via cascading database constraints.
 *
 * @see com.mehdi.taskflow.user.UserService#deleteAccount(DeleteAccountRequest, HttpServletResponse)
 * @see com.mehdi.taskflow.user.UserController
 */
@Schema(
        name = "DeleteAccountRequest",
        description = "Request body for permanently deleting the authenticated user's account")
public class DeleteAccountRequest {

    /**
     * The user's current password used as confirmation before deletion. Verified against the stored
     * BCrypt hash before proceeding.
     */
    @Schema(
            description =
                    "The user's current password. Required as confirmation before permanent deletion.",
            example = "MyPassword@2026",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "{validation.password.current.required}")
    private String password;

    /** Default constructor required for JSON deserialization. */
    public DeleteAccountRequest() {}

    /**
     * @return the confirmation password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the confirmation password
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
