package com.mehdi.taskflow.user;

import com.mehdi.taskflow.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller handling authenticated user profile operations.
 *
 * <p>Exposes endpoints for retrieving and managing the profile
 * of the currently authenticated user.
 * All endpoints require a valid JWT token.</p>
 *
 * <p>All responses are produced in {@code application/json} format.</p>
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
     * <p>Delegates to {@link UserService#getUserProfile()} which resolves
     * the authenticated user from the current {@link org.springframework.security.core.context.SecurityContext}.</p>
     *
     * @return {@code 200 OK} with the authenticated user's public profile as {@link UserResponse},
     *         or {@code 401 Unauthorized} if no valid JWT token is present
     */
    @Override
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getProfile() {
        return ResponseEntity.ok(userService.getUserProfile());
    }
}
