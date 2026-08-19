package com.mehdi.taskflow.user;

import com.mehdi.taskflow.user.dto.ChangePasswordRequest;
import com.mehdi.taskflow.user.dto.DeleteAccountRequest;
import com.mehdi.taskflow.user.dto.UpdateProfileRequest;
import com.mehdi.taskflow.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller handling authenticated user profile operations.
 *
 * <p>Exposes endpoints for retrieving and managing the profile of the currently authenticated user.
 * All endpoints require a valid JWT token.
 *
 * <p>All responses are produced in {@code application/json} format.
 *
 * @see UserService
 */
@RestController
@RequestMapping(value = "/api/users", produces = "application/json")
public class UserController implements UserControllerApi {

    private final UserService userService;

    /**
     * Constructs a new {@code UserController} with its required dependency.
     *
     * @param userService service handling user profile operations
     */
    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Returns the public profile of the currently authenticated user.
     *
     * <p>Delegates to {@link UserService#getUserProfile()} which resolves the authenticated user
     * from the current {@link org.springframework.security.core.context.SecurityContext}.
     *
     * @return {@code 200 OK} with the authenticated user's public profile as {@link UserResponse},
     *     or {@code 401 Unauthorized} if no valid JWT token is present
     */
    @Override
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }

    /**
     * Changes the password of the currently authenticated user.
     *
     * <p>Verifies the current password, applies the new password encoded with BCrypt, and revokes
     * all active refresh tokens to invalidate existing sessions.
     *
     * @param request the change password data — current and new passwords
     * @return {@code 204 No Content} on success, {@code 400 Bad Request} if validation fails or
     *     current password is incorrect, or {@code 401 Unauthorized} if no valid JWT token is
     *     present
     */
    @Override
    @PostMapping("/me/password")
    public ResponseEntity<Void> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.noContent().build();
    }

    /**
     * Updates the profile of the currently authenticated user.
     *
     * <p>Validates uniqueness of the new username and email before applying changes. The username
     * is sanitized before persistence.
     *
     * @param request the updated profile data — new username and email
     * @return {@code 200 OK} with the updated user profile as {@link UserResponse}, {@code 400 Bad
     *     Request} if validation fails or username/email is already taken, or {@code 401
     *     Unauthorized} if no valid JWT token is present
     */
    @Override
    @PutMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(request));
    }

    /**
     * Permanently deletes the account of the currently authenticated user.
     *
     * <p>Requires password confirmation. Associated data is removed by the database itself through
     * {@code ON DELETE} clauses declared on the foreign keys: owned projects, their tasks, refresh
     * tokens and provider links. Tasks assigned to the user inside a project owned by someone else
     * are preserved and become unassigned.
     *
     * <p>Both HttpOnly session cookies are cleared after successful deletion.
     *
     * @param request the deletion confirmation data — user's current password
     * @param response the HTTP response used to clear the session cookies
     * @return {@code 204 No Content} on success, {@code 400 Bad Request} if validation fails or
     *     password is incorrect, or {@code 401 Unauthorized} if no valid JWT token is present
     */
    @Override
    @DeleteMapping("/me")
    public ResponseEntity<Void> deleteAccount(
            @Valid @RequestBody DeleteAccountRequest request, HttpServletResponse response) {
        userService.deleteAccount(request, response);
        return ResponseEntity.noContent().build();
    }
}
