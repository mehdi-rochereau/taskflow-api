package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.user.User;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity representing a refresh token issued to an authenticated user.
 *
 * <p>Refresh tokens are used to obtain new JWT access tokens without
 * requiring the user to re-authenticate. Each refresh token is single-use —
 * upon use, it is revoked and a new one is issued (rotation).</p>
 *
 * <p>Refresh tokens are invalidated upon logout or when the user account
 * is deleted — enforced by {@code ON DELETE CASCADE} on the foreign key.</p>
 *
 * @see User
 * @see RefreshTokenRepository
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The unique refresh token string — a UUID generated at creation time.
     */
    @Column(nullable = false, unique = true)
    private String token;

    /**
     * The user this refresh token belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Expiration timestamp — after this date the token is no longer valid.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Whether this token has been revoked.
     *
     * <p>A token is revoked after use (rotation) or upon logout.
     * Revoked tokens are kept in the database for audit purposes.</p>
     */
    @Column(nullable = false)
    private boolean revoked = false;

    /**
     * Timestamp of refresh token creation. Set automatically on first persist.
     * Cannot be updated after creation.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Sets the {@code createdAt} timestamp before persisting.
     */
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /** @return the unique identifier */
    public Long getId() { return id; }

    /** @return the refresh token string */
    public String getToken() { return token; }

    /** @param token the refresh token string */
    public void setToken(String token) { this.token = token; }

    /** @return the user this token belongs to */
    public User getUser() { return user; }

    /** @param user the user this token belongs to */
    public void setUser(User user) { this.user = user; }

    /** @return the expiration timestamp */
    public LocalDateTime getExpiresAt() { return expiresAt; }

    /** @param expiresAt the expiration timestamp */
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }

    /** @return whether this token has been revoked */
    public boolean isRevoked() { return revoked; }

    /** @param revoked whether this token has been revoked */
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    /** @return the creation timestamp */
    public LocalDateTime getCreatedAt() { return createdAt; }
}