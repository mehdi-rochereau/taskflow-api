package com.mehdi.taskflow.user;

import com.mehdi.taskflow.user.dto.ChangePasswordRequest;
import com.mehdi.taskflow.user.dto.DeleteAccountRequest;
import com.mehdi.taskflow.user.dto.UpdateProfileRequest;
import com.mehdi.taskflow.user.dto.UserResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenAPI documentation interface for {@link UserController}.
 *
 * <p>Declares all Swagger/OpenAPI annotations for user profile endpoints, keeping {@link
 * UserController} clean and focused on business logic. This separation follows the Interface
 * Segregation Principle — documentation concerns are isolated from routing and delegation logic.
 *
 * <p>All endpoints defined here require the {@code jwt} HttpOnly cookie.
 *
 * @see UserController
 * @see UserService
 */
@Tag(
        name = "User",
        description =
                "Endpoints for managing the authenticated user's profile. All endpoints require a valid JWT token.")
public interface UserControllerApi {

    /**
     * Returns the authenticated user's public profile.
     *
     * <p>Implemented by {@link UserController}; this interface carries the OpenAPI annotations
     * only. The identity comes from the security context, so there is no path variable and no way
     * to read another account through this endpoint.
     *
     * @return {@code 200 OK} with the caller's identifier, username, email, role and creation date.
     *     The password hash is never exposed
     */
    @Operation(
            summary = "Get authenticated user profile",
            description =
                    """
                    Returns the public profile of the currently authenticated user.

                    The response never includes the password or internal fields.
                    """,
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Profile retrieved successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "Success",
                                                        value =
                                                                """
                                                    {
                                                      "id": 1,
                                                      "username": "mehdi",
                                                      "email": "mehdi@example.com",
                                                      "role": "ROLE_USER",
                                                      "createdAt": "2026-04-01T10:00:00"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected server error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """)))
            })
    ResponseEntity<UserResponse> getProfile();

    /**
     * Changes the authenticated user's password.
     *
     * <p>Implemented by {@link UserController}; this interface carries the OpenAPI annotations
     * only. Every refresh token is revoked on success, so all sessions on all devices end and the
     * user must authenticate again, including on the device that made the change.
     *
     * @param request the current and new passwords
     * @return {@code 204 No Content}, or {@code 400} if the current password is wrong or the new
     *     one is identical to it
     */
    @Operation(
            summary = "Change authenticated user password",
            description =
                    """
                    Changes the password of the currently authenticated user.

                    **Requirements:**
                    - `currentPassword` must match the stored password
                    - `newPassword` must be different from the current password
                    - `newPassword` must meet strength requirements:
                      at least 8 characters, 1 uppercase, 1 digit, 1 special character

                    **Security:** All active refresh tokens are revoked after a successful
                    password change — the user must re-authenticate on all devices.
                    """,
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Password changed successfully — all sessions revoked",
                        content = @Content),
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation failed or new password identical to current",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "newPassword": ["Password must be at least 8 characters and contain at least one uppercase letter, one digit and one special character"]
                                                              }
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "Same password",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "New password must be different from the current password"
                                                            }
                                                            """)
                                        })),
                @ApiResponse(
                        responseCode = "422",
                        description = "Current password does not match the stored hash",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "Incorrect current password",
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 422,
                                                      "message": "Current password is incorrect"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected server error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """)))
            })
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request);

    /**
     * Updates the authenticated user's username and email.
     *
     * <p>Implemented by {@link UserController}; this interface carries the OpenAPI annotations
     * only. Uniqueness is checked against other accounts only, so resubmitting an unchanged value
     * is not rejected. The username is sanitized before persistence.
     *
     * @param request the new profile data, username and email
     * @return {@code 200 OK} with the updated profile, or {@code 400} if either value is already
     *     held by another account
     */
    @Operation(
            summary = "Update authenticated user profile",
            description =
                    """
                    Updates the username and email of the currently authenticated user.

                    **Uniqueness:** The new username and email must not already be taken
                    by another account. Re-submitting unchanged values is allowed.

                    **Sanitization:** The username is sanitized before persistence
                    to prevent XSS attacks.
                    """,
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "200",
                        description = "Profile updated successfully",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        schema = @Schema(implementation = UserResponse.class),
                                        examples =
                                                @ExampleObject(
                                                        name = "Success",
                                                        value =
                                                                """
                                                    {
                                                      "id": 1,
                                                      "username": "mehdi_updated",
                                                      "email": "mehdi.updated@example.com",
                                                      "role": "ROLE_USER",
                                                      "createdAt": "2026-04-01T10:00:00"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "400",
                        description =
                                "Validation failed or username/email already taken by another account",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "username": ["Username is required"],
                                                                "email": ["Invalid email address"]
                                                              }
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "Username taken",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This username is already taken by another account"
                                                            }
                                                            """),
                                            @ExampleObject(
                                                    name = "Email taken",
                                                    value =
                                                            """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This email is already in use by another account"
                                                            }
                                                            """)
                                        })),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected server error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """)))
            })
    ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request);

    /**
     * Permanently deletes the authenticated user's account.
     *
     * <p>Implemented by {@link UserController}; this interface carries the OpenAPI annotations
     * only. Password confirmation is required, so a stolen session alone cannot destroy an account.
     * Owned projects and their tasks follow, removed by the database through {@code ON DELETE
     * CASCADE}; tasks merely assigned to the user inside someone else's project survive and become
     * unassigned.
     *
     * @param request the deletion confirmation, carrying the account password
     * @param response the HTTP response used to clear both authentication cookies
     * @return {@code 204 No Content}. The operation is irreversible
     */
    @Operation(
            summary = "Delete authenticated user account",
            description =
                    """
                    Permanently deletes the authenticated user's account.

                    **Requires password confirmation** to prevent accidental or unauthorized deletion.

                    **Deleted with the account:**
                    - The user account
                    - All projects owned by the user
                    - All tasks belonging to those projects
                    - All refresh tokens issued to the user
                    - All linked authentication providers

                    **Kept:** tasks assigned to the user inside a project owned by someone
                    else are preserved and become unassigned. A task belongs to its project,
                    not to its assignee.

                    **Session:** both `jwt` and `refreshToken` HttpOnly cookies are cleared
                    after successful deletion.

                    ⚠️ **This operation is irreversible.**
                    """,
            parameters = {@Parameter(ref = "#/components/parameters/Accept-Language")},
            responses = {
                @ApiResponse(
                        responseCode = "204",
                        description = "Account permanently deleted — session cookies cleared",
                        content = @Content),
                @ApiResponse(
                        responseCode = "400",
                        description = "Validation failed",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "Validation error",
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 400,
                                                      "errors": {
                                                        "password": ["Current password is required"]
                                                      }
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "422",
                        description = "Confirmation password does not match the stored hash",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        name = "Incorrect password",
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 422,
                                                      "message": "Password is incorrect — account deletion cancelled"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "401",
                        description = "Missing or invalid JWT token",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """))),
                @ApiResponse(
                        responseCode = "500",
                        description = "Unexpected server error",
                        content =
                                @Content(
                                        mediaType = "application/json",
                                        examples =
                                                @ExampleObject(
                                                        value =
                                                                """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """)))
            })
    ResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request, HttpServletResponse response);
}
