package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.config.AuditService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Scheduled service responsible for purging expired and revoked refresh tokens.
 *
 * <p>Runs daily at 2:00 AM to delete refresh tokens that are either:</p>
 * <ul>
 *   <li>Expired — {@code expires_at} is in the past</li>
 *   <li>Revoked — marked as revoked after use or logout</li>
 * </ul>
 *
 * <p>This prevents unbounded growth of the {@code refresh_tokens} table
 * while preserving active tokens for authenticated users.</p>
 *
 * <p>Requires {@code @EnableScheduling} on {@link com.mehdi.taskflow.TaskflowApiApplication}.</p>
 *
 * @see RefreshTokenRepository#deleteAllExpiredOrRevoked(LocalDateTime)
 * @see AuditService#logTokenPurge(int)
 */
@Service
public class TokenCleanupService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final AuditService auditService;

    /**
     * Constructs a new {@code TokenCleanupService} with its required dependencies.
     *
     * @param refreshTokenRepository repository for refresh token persistence
     * @param auditService           service for logging audit events
     */
    public TokenCleanupService(RefreshTokenRepository refreshTokenRepository,
                               AuditService auditService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.auditService = auditService;
    }

    /**
     * Purges all expired and revoked refresh tokens from the database.
     *
     * <p>Scheduled to run every day at 2:00 AM via cron expression
     * {@code 0 0 2 * * *}. The purge is transactional — if an error occurs,
     * no tokens are deleted.</p>
     *
     * <p>The number of deleted tokens is logged via {@link AuditService}
     * for monitoring and audit purposes.</p>
     */
    @Scheduled(cron = "0 0 2 * * *")
    @Transactional
    public void purgeExpiredAndRevokedTokens() {
        int deleted = refreshTokenRepository.deleteAllExpiredOrRevoked(LocalDateTime.now());
        auditService.logTokenPurge(deleted);
    }
}