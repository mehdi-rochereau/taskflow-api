package com.mehdi.taskflow.user.dto;

import java.time.LocalDateTime;

/**
 * DTO representing the authenticated user's public profile.
 *
 * <p>Returned by {@code GET /api/users/me} — exposes only non-sensitive fields.
 * The password, refresh tokens, and OAuth2 provider details are never included.</p>
 *
 * <p>This class is immutable — all fields are final and set via the constructor.</p>
 */
public class UserResponse {

    private final Long id;
    private final String username;
    private final String email;
    private final String role;
    private final LocalDateTime createdAt;

    /**
     * Constructs a new {@code UserResponse} with all required fields.
     *
     * @param id        the user's unique identifier
     * @param username  the user's unique username
     * @param email     the user's email address
     * @param role      the user's role (e.g. {@code ROLE_USER})
     * @param createdAt the account creation timestamp
     */
    public UserResponse(Long id, String username, String email,
                        String role, LocalDateTime createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdAt = createdAt;
    }

    /** @return the user's unique identifier */
    public Long getId() { return id; }

    /** @return the user's unique username */
    public String getUsername() { return username; }

    /** @return the user's email address */
    public String getEmail() { return email; }

    /** @return the user's role (e.g. {@code ROLE_USER}) */
    public String getRole() { return role; }

    /** @return the account creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }
}
