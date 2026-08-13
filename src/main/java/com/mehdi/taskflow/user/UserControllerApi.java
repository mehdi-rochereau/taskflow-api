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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * OpenAPI documentation interface for {@link UserController}.
 *
 * <p>Declares all Swagger/OpenAPI annotations for user profile endpoints,
 * keeping {@link UserController} clean and focused on business logic.
 * This separation follows the Interface Segregation Principle — documentation
 * concerns are isolated from routing and delegation logic.</p>
 *
 * <p>All endpoints defined here require a valid JWT token passed as a Bearer token
 * in the {@code Authorization} header or via the {@code jwt} HttpOnly cookie.</p>
 *
 * @see UserController
 * @see UserService
 */
@Tag(
        name = "User",
        description = "Endpoints for managing the authenticated user's profile. All endpoints require a valid JWT token."
)
@SecurityRequirement(name = "bearerAuth")
public interface UserControllerApi {

    @Operation(
            summary = "Get authenticated user profile",
            description = """
                    Returns the public profile of the currently authenticated user.
                    
                    The response never includes the password or internal fields.
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Profile retrieved successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "username": "mehdi",
                                                      "email": "mehdi@example.com",
                                                      "role": "ROLE_USER",
                                                      "createdAt": "2026-04-01T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<UserResponse> getProfile();

    @Operation(
            summary = "Change authenticated user password",
            description = """
                    Changes the password of the currently authenticated user.
                    
                    **Requirements:**
                    - `currentPassword` must match the stored password
                    - `newPassword` must be different from the current password
                    - `newPassword` must meet strength requirements:
                      at least 8 characters, 1 uppercase, 1 digit, 1 special character
                    
                    **Security:** All active refresh tokens are revoked after a successful
                    password change — the user must re-authenticate on all devices.
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Password changed successfully — all sessions revoked",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed, current password incorrect or new password identical to current",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "newPassword": ["Password must be at least 8 characters and contain at least one uppercase letter, one digit and one special character"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Incorrect current password",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "Current password is incorrect"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Same password",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "New password must be different from the current password"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request);

    @Operation(
            summary = "Update authenticated user profile",
            description = """
                    Updates the username and email of the currently authenticated user.
                    
                    **Uniqueness:** The new username and email must not already be taken
                    by another account. Re-submitting unchanged values is allowed.
                    
                    **Sanitization:** The username is sanitized before persistence
                    to prevent XSS attacks.
                    """,
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "Profile updated successfully",
                            content = @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = UserResponse.class),
                                    examples = @ExampleObject(
                                            name = "Success",
                                            value = """
                                                    {
                                                      "id": 1,
                                                      "username": "mehdi_updated",
                                                      "email": "mehdi.updated@example.com",
                                                      "role": "ROLE_USER",
                                                      "createdAt": "2026-04-01T10:00:00"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed or username/email already taken by another account",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "username": ["Username is required"],
                                                                "email": ["Invalid email address"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Username taken",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This username is already taken by another account"
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Email taken",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "This email is already in use by another account"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<UserResponse> updateProfile(@Valid @RequestBody UpdateProfileRequest request);

    @Operation(
            summary = "Delete authenticated user account",
            description = """
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
            parameters = {
                    @Parameter(ref = "#/components/parameters/Accept-Language")
            },
            responses = {
                    @ApiResponse(
                            responseCode = "204",
                            description = "Account permanently deleted — session cookies cleared",
                            content = @Content
                    ),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Validation failed or password incorrect",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = {
                                            @ExampleObject(
                                                    name = "Validation error",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "errors": {
                                                                "password": ["Current password is required"]
                                                              }
                                                            }
                                                            """
                                            ),
                                            @ExampleObject(
                                                    name = "Incorrect password",
                                                    value = """
                                                            {
                                                              "timestamp": "2026-04-18T10:00:00",
                                                              "status": 400,
                                                              "message": "Password is incorrect — account deletion cancelled"
                                                            }
                                                            """
                                            )
                                    }
                            )
                    ),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Missing or invalid JWT token",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "status": 401,
                                                      "message": "Authentication required"
                                                    }
                                                    """
                                    )
                            )
                    ),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Unexpected server error",
                            content = @Content(
                                    mediaType = "application/json",
                                    examples = @ExampleObject(
                                            value = """
                                                    {
                                                      "timestamp": "2026-04-18T10:00:00",
                                                      "status": 500,
                                                      "message": "An unexpected error occurred"
                                                    }
                                                    """
                                    )
                            )
                    )
            }
    )
    ResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request,
            HttpServletResponse response);
}
