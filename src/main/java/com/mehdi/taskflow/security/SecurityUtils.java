package com.mehdi.taskflow.security;

import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility component for resolving the currently authenticated user.
 *
 * <p>Reads the username from the {@link SecurityContextHolder} — populated
 * by {@link JwtFilter} after successful JWT validation — and loads the
 * corresponding {@link User} from the database.</p>
 *
 * <p>Centralizes authentication context access to avoid duplication across
 * service classes and simplify unit testing via mocking.</p>
 *
 * <p>Usage example:</p>
 * <pre>{@code
 * User currentUser = securityUtils.getCurrentUser();
 * }</pre>
 *
 * @see JwtFilter
 * @see SecurityContextHolder
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Constructs a new {@code SecurityUtils} with its required dependency.
     *
     * @param userRepository repository used to load the authenticated user
     */
    public SecurityUtils(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Returns the {@link User} entity corresponding to the currently authenticated principal.
     *
     * <p>Extracts the username from the active {@link org.springframework.security.core.Authentication}
     * in the {@link SecurityContextHolder}, then queries the database for the matching user.</p>
     *
     * @return the authenticated user
     * @throws ResourceNotFoundException if no user matches the authenticated username —
     *         this should not occur in normal operation as the JWT filter already
     *         validates the token against an existing user
     * @throws IllegalStateException if called outside an authenticated request context
     */
    public User getCurrentUser() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}