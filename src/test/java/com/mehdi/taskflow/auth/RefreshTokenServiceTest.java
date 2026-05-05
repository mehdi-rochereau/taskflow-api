package com.mehdi.taskflow.auth;

import com.mehdi.taskflow.config.CookieUtils;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.dto.AuthResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RefreshTokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private MessageService messageService;

    @Mock
    private JwtService jwtService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @InjectMocks
    private RefreshTokenService refreshTokenService;

    private User user;
    private RefreshToken validToken;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "expirationDays", 7);
        ReflectionTestUtils.setField(refreshTokenService, "cookieSecure", false);
        ReflectionTestUtils.setField(refreshTokenService, "jwtExpiration", 900_000L);

        user = new User();
        user.setUsername("mehdi");
        user.setEmail("mehdi@example.com");

        validToken = new RefreshToken();
        validToken.setToken("valid-uuid-token");
        validToken.setUser(user);
        validToken.setRevoked(false);
        validToken.setExpiresAt(LocalDateTime.now().plusDays(7));
    }

    private void givenRefreshTokenCookie(String tokenValue) {
        when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("refreshToken", tokenValue)});
    }

    // =========================================================================
    // generateRefreshToken
    // =========================================================================

    @Nested
    class GenerateRefreshToken {

        @Test
        void generateRefreshToken_shouldGenerateAndPersistToken_whenCalled() {
            // GIVEN
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // WHEN
            RefreshToken result = refreshTokenService.generateRefreshToken(user);

            // THEN
            assertNotNull(result.getToken());
            assertFalse(result.isRevoked());
            assertEquals(user, result.getUser());
            assertTrue(result.getExpiresAt().isAfter(LocalDateTime.now()));
            verify(refreshTokenRepository).save(result);
        }

        @Test
        void generateRefreshToken_shouldSetExpirationTo7Days_whenCalled() {
            // GIVEN
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // WHEN
            RefreshToken result = refreshTokenService.generateRefreshToken(user);

            // THEN
            LocalDateTime expectedExpiry = LocalDateTime.now().plusDays(7);
            assertTrue(result.getExpiresAt().isAfter(expectedExpiry.minusSeconds(5)));
            assertTrue(result.getExpiresAt().isBefore(expectedExpiry.plusSeconds(5)));
            verify(refreshTokenRepository).save(result);
        }

        @Test
        void generateRefreshToken_shouldGenerateUniqueToken_whenCalledTwice() {
            // GIVEN
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));

            // WHEN
            RefreshToken token1 = refreshTokenService.generateRefreshToken(user);
            RefreshToken token2 = refreshTokenService.generateRefreshToken(user);

            // THEN
            assertNotEquals(token1.getToken(), token2.getToken());
            verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
        }
    }

    // =========================================================================
    // addRefreshTokenCookie
    // =========================================================================

    @Nested
    class AddRefreshTokenCookie {

        @Test
        void addRefreshTokenCookie_shouldCallCookieUtils_withCorrectParameters() {
            // GIVEN / WHEN / THEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.addRefreshTokenCookie(response, "my-token");

                mocked.verify(() -> CookieUtils.addCookie(
                        response,
                        "refreshToken",
                        "my-token",
                        "/api/auth",
                        7 * 24 * 60 * 60,
                        false
                ));
            }
        }
    }

    // =========================================================================
    // refresh
    // =========================================================================

    @Nested
    class Refresh {

        @Test
        void refresh_shouldReturnAuthResponse_whenTokenIsValid() {
            // GIVEN
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(user)).thenReturn("new-jwt-token");

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                AuthResponse result = refreshTokenService.refresh(request, response);

                // THEN
                assertEquals("new-jwt-token", result.getToken());
                assertEquals("mehdi", result.getUsername());
                assertEquals("mehdi@example.com", result.getEmail());
                verify(refreshTokenRepository).findByToken("valid-uuid-token");
                verify(jwtService).generateToken(user);
                verify(messageService, never()).get(any());
            }
        }

        @Test
        void refresh_shouldRevokeOldToken_whenTokenIsValid() {
            // GIVEN
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(user)).thenReturn("new-jwt");

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.refresh(request, response);

                // THEN
                ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
                verify(refreshTokenRepository, atLeastOnce()).save(captor.capture());
                assertTrue(captor.getAllValues().stream().anyMatch(RefreshToken::isRevoked));
            }
        }

        @Test
        void refresh_shouldWriteBothCookies_whenTokenIsValid() {
            // GIVEN
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(user)).thenReturn("new-jwt");

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.refresh(request, response);

                // THEN
                mocked.verify(() -> CookieUtils.addCookie(eq(response), eq("jwt"), eq("new-jwt"), eq("/api"), anyInt(), eq(false)));
                mocked.verify(() -> CookieUtils.addCookie(eq(response), eq("refreshToken"), anyString(), eq("/api/auth"), anyInt(), eq(false)));
            }
        }

        @Test
        void refresh_shouldThrow_whenCookieIsAbsent() {
            // GIVEN
            when(request.getCookies()).thenReturn(null);
            when(messageService.get("error.refresh.token.not.found")).thenReturn("Token introuvable");

            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token introuvable", ex.getMessage());
            verify(refreshTokenRepository, never()).findByToken(any());
            verify(messageService).get("error.refresh.token.not.found");
        }

        @Test
        void refresh_shouldThrow_whenCookiesArrayIsEmpty() {
            // GIVEN
            when(request.getCookies()).thenReturn(new Cookie[]{});
            when(messageService.get("error.refresh.token.not.found")).thenReturn("Token introuvable");

            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token introuvable", ex.getMessage());
            verify(refreshTokenRepository, never()).findByToken(any());
            verify(messageService).get("error.refresh.token.not.found");
        }

        @Test
        void refresh_shouldThrow_whenTokenNotFoundInDatabase() {
            // GIVEN
            givenRefreshTokenCookie("unknown-token");
            when(refreshTokenRepository.findByToken("unknown-token")).thenReturn(Optional.empty());
            when(messageService.get("error.refresh.token.not.found")).thenReturn("Token introuvable");

            // WHEN
            ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token introuvable", ex.getMessage());
            verify(refreshTokenRepository).findByToken("unknown-token");
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenRepository, never()).save(any());
            verify(messageService).get("error.refresh.token.not.found");
        }

        @Test
        void refresh_shouldThrow_whenTokenIsRevoked() {
            // GIVEN
            validToken.setRevoked(true);
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(messageService.get("error.refresh.token.revoked")).thenReturn("Token révoqué");

            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token révoqué", ex.getMessage());
            verify(refreshTokenRepository).findByToken("valid-uuid-token");
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenRepository, never()).save(any());
            verify(messageService, never()).get("error.refresh.token.not.found");
            verify(messageService).get("error.refresh.token.revoked");
        }

        @Test
        void refresh_shouldThrow_whenTokenIsExpired() {
            // GIVEN
            validToken.setExpiresAt(LocalDateTime.now().minusSeconds(1));
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(messageService.get("error.refresh.token.expired")).thenReturn("Token expiré");

            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token expiré", ex.getMessage());
            verify(refreshTokenRepository).findByToken("valid-uuid-token");
            verify(jwtService, never()).generateToken(any());
            verify(refreshTokenRepository, never()).save(any());
            verify(messageService, never()).get("error.refresh.token.not.found");
            verify(messageService, never()).get("error.refresh.token.revoked");
            verify(messageService).get("error.refresh.token.expired");
        }

        @Test
        void refresh_shouldThrow_whenRefreshTokenCookieAbsentAmongOtherCookies() {
            // GIVEN
            when(request.getCookies()).thenReturn(new Cookie[]{new Cookie("jwt", "some-jwt")});
            when(messageService.get("error.refresh.token.not.found")).thenReturn("Token introuvable");

            // WHEN
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> refreshTokenService.refresh(request, response));

            // THEN
            assertEquals("Token introuvable", ex.getMessage());
            verify(refreshTokenRepository, never()).findByToken(any());
            verify(messageService).get("error.refresh.token.not.found");
        }

        @Test
        void refresh_shouldExtractCorrectCookie_whenMultipleCookiesPresent() {
            // GIVEN
            when(request.getCookies()).thenReturn(new Cookie[]{
                    new Cookie("jwt", "some-jwt"),
                    new Cookie("refreshToken", "valid-uuid-token")
            });
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));
            when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(i -> i.getArgument(0));
            when(jwtService.generateToken(user)).thenReturn("new-jwt");

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                AuthResponse result = refreshTokenService.refresh(request, response);

                // THEN
                assertEquals("new-jwt", result.getToken());
                verify(refreshTokenRepository).findByToken("valid-uuid-token");
                verify(messageService, never()).get(any());
            }
        }
    }

    // =========================================================================
    // logout
    // =========================================================================

    @Nested
    class Logout {

        @Test
        void logout_shouldRevokeAllUserTokens_whenCookieIsPresent() {
            // GIVEN
            givenRefreshTokenCookie("valid-uuid-token");
            when(refreshTokenRepository.findByToken("valid-uuid-token")).thenReturn(Optional.of(validToken));

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.logout(request, response);

                // THEN
                verify(refreshTokenRepository).findByToken("valid-uuid-token");
                verify(refreshTokenRepository).revokeAllByUser(user);
                mocked.verify(() -> CookieUtils.clearCookie(response, "jwt", "/api", false));
                mocked.verify(() -> CookieUtils.clearCookie(response, "refreshToken", "/api/auth", false));
            }
        }

        @Test
        void logout_shouldClearBothCookies_whenCookieIsAbsent() {
            // GIVEN
            when(request.getCookies()).thenReturn(null);

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.logout(request, response);

                // THEN
                verify(refreshTokenRepository, never()).findByToken(any());
                verify(refreshTokenRepository, never()).revokeAllByUser(any());
                mocked.verify(() -> CookieUtils.clearCookie(response, "jwt", "/api", false));
                mocked.verify(() -> CookieUtils.clearCookie(response, "refreshToken", "/api/auth", false));
            }
        }

        @Test
        void logout_shouldNotRevokeTokens_whenTokenNotInDatabase() {
            // GIVEN
            givenRefreshTokenCookie("ghost-token");
            when(refreshTokenRepository.findByToken("ghost-token")).thenReturn(Optional.empty());

            // WHEN
            try (MockedStatic<CookieUtils> mocked = mockStatic(CookieUtils.class)) {
                refreshTokenService.logout(request, response);

                // THEN
                verify(refreshTokenRepository).findByToken("ghost-token");
                verify(refreshTokenRepository, never()).revokeAllByUser(any());
                mocked.verify(() -> CookieUtils.clearCookie(response, "jwt", "/api", false));
                mocked.verify(() -> CookieUtils.clearCookie(response, "refreshToken", "/api/auth", false));
            }
        }
    }
}