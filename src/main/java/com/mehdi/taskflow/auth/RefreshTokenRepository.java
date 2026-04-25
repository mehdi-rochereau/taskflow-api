package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.auth.RefreshToken;
import com.mehdi.taskflow.user.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Repository for {@link RefreshToken} entities.
 *
 * <p>Provides lookup and revocation methods for refresh tokens.</p>
 */
@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    /**
     * Finds a refresh token by its token string.
     *
     * @param token the refresh token string
     * @return the matching {@link RefreshToken} if found
     */
    Optional<RefreshToken> findByToken(String token);

    /**
     * Revokes all active refresh tokens for a given user.
     * Used during logout to invalidate all sessions.
     *
     * @param user the user whose tokens should be revoked
     */
    @Modifying
    @Query("UPDATE RefreshToken rt SET rt.revoked = true WHERE rt.user = :user AND rt.revoked = false")
    void revokeAllByUser(@Param("user") User user);

    /**
     * Deletes all refresh tokens that have expired or have been revoked.
     * Used by the scheduled cleanup job to prevent unbounded table growth.
     *
     * @param expiresAt tokens expired before this timestamp will be deleted
     * @return the number of deleted tokens
     */
    @Modifying
    @Query("DELETE FROM RefreshToken rt WHERE rt.expiresAt < :expiresAt OR rt.revoked = true")
    int deleteAllExpiredOrRevoked(@Param("expiresAt") LocalDateTime expiresAt);
}