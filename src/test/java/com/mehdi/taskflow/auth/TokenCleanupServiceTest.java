package com.mehdi.taskflow.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mehdi.taskflow.config.AuditService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class TokenCleanupServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;

    @Mock private AuditService auditService;

    @InjectMocks private TokenCleanupService tokenCleanupService;

    @Test
    void purgeExpiredAndRevokedTokens_shouldDeleteAndLogTheCount() {
        // GIVEN
        when(refreshTokenRepository.deleteAllExpiredOrRevoked(any(LocalDateTime.class)))
                .thenReturn(12);

        // WHEN
        tokenCleanupService.purgeExpiredAndRevokedTokens();

        // THEN
        // The logged count must be the value the repository reported, not a
        // constant: an audit line stating a purge that deleted nothing would be
        // worse than no line at all.
        verify(refreshTokenRepository).deleteAllExpiredOrRevoked(any(LocalDateTime.class));
        verify(auditService).logTokenPurge(12);
    }

    @Test
    void purgeExpiredAndRevokedTokens_shouldLogZero_whenNothingWasExpired() {
        // GIVEN
        // The nominal case on a healthy database: the line is still written, so
        // that a silent scheduler can be told apart from an empty purge.
        when(refreshTokenRepository.deleteAllExpiredOrRevoked(any(LocalDateTime.class)))
                .thenReturn(0);

        // WHEN
        tokenCleanupService.purgeExpiredAndRevokedTokens();

        // THEN
        verify(auditService).logTokenPurge(0);
    }

    @Test
    void purgeExpiredAndRevokedTokens_shouldPassTheCurrentInstant() {
        // GIVEN
        // The cutoff decides which tokens are considered expired. Passing a
        // fixed or stale instant would either spare expired tokens or delete
        // valid ones, and nothing else in the code would show it.
        LocalDateTime before = LocalDateTime.now();
        when(refreshTokenRepository.deleteAllExpiredOrRevoked(any(LocalDateTime.class)))
                .thenReturn(3);
        ArgumentCaptor<LocalDateTime> cutoff = ArgumentCaptor.forClass(LocalDateTime.class);

        // WHEN
        tokenCleanupService.purgeExpiredAndRevokedTokens();

        // THEN
        verify(refreshTokenRepository).deleteAllExpiredOrRevoked(cutoff.capture());
        LocalDateTime after = LocalDateTime.now();
        assertTrue(!cutoff.getValue().isBefore(before) && !cutoff.getValue().isAfter(after));
    }

    @Test
    void purgeExpiredAndRevokedTokens_shouldDeleteBeforeLogging() {
        // GIVEN
        when(refreshTokenRepository.deleteAllExpiredOrRevoked(any(LocalDateTime.class)))
                .thenReturn(5);

        // WHEN
        tokenCleanupService.purgeExpiredAndRevokedTokens();

        // THEN
        InOrder inOrder = inOrder(refreshTokenRepository, auditService);
        inOrder.verify(refreshTokenRepository).deleteAllExpiredOrRevoked(any(LocalDateTime.class));
        inOrder.verify(auditService).logTokenPurge(5);
    }

    @Test
    void purgeExpiredAndRevokedTokens_shouldNotLog_whenDeletionFails() {
        // GIVEN
        // The method is @Transactional, so a failure rolls the deletion back.
        // An audit line written anyway would claim a purge that never happened.
        when(refreshTokenRepository.deleteAllExpiredOrRevoked(any(LocalDateTime.class)))
                .thenThrow(new IllegalStateException("connection lost"));

        // WHEN & THEN
        assertThrows(
                IllegalStateException.class,
                () -> tokenCleanupService.purgeExpiredAndRevokedTokens());
        verify(auditService, never()).logTokenPurge(org.mockito.ArgumentMatchers.anyInt());
    }
}
