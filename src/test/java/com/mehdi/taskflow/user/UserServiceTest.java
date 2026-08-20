package com.mehdi.taskflow.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.contains;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.mehdi.taskflow.auth.RefreshToken;
import com.mehdi.taskflow.auth.RefreshTokenService;
import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SanitizationService;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.SecurityUtils;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.ChangePasswordRequest;
import com.mehdi.taskflow.user.dto.DeleteAccountRequest;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import com.mehdi.taskflow.user.dto.UpdateProfileRequest;
import com.mehdi.taskflow.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;

    @Mock private PasswordEncoder passwordEncoder;

    @Mock private JwtService jwtService;

    @Mock private AuthenticationManager authenticationManager;

    @Mock private HttpServletResponse httpServletResponse;

    @Mock private MessageService messageService;

    @Mock private AuditService auditService;

    @Mock private RefreshTokenService refreshTokenService;

    @Mock private SanitizationService sanitizationService;

    @Mock private SecurityUtils securityUtils;

    @InjectMocks private UserService userService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User user;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setUsername("mehdi");
        registerRequest.setEmail("mehdi@test.com");
        registerRequest.setPassword("password123");

        loginRequest = new LoginRequest();
        loginRequest.setIdentifier("mehdi");
        loginRequest.setPassword("password123");

        user = new User();
        user.setId(1L);
        user.setUsername("mehdi");
        user.setEmail("mehdi@test.com");
        user.setPassword("hashedPassword");
        user.setRole("ROLE_USER");
    }

    @Test
    void register_shouldCreateUserAndIssueCookies() {
        // GIVEN
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class)))
                .thenReturn(mockRefreshToken);
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(false);
        when(sanitizationService.sanitizeAndLog(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.register(registerRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository).existsByUsername("mehdi");
        verify(userRepository).existsByEmail("mehdi@test.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository)
                .save(
                        argThat(
                                u ->
                                        u.getUsername().equals("mehdi")
                                                && u.getEmail().equals("mehdi@test.com")
                                                && u.getPassword().equals("hashedPassword")
                                                && u.getRole().equals("ROLE_USER")));
        verify(jwtService)
                .generateToken(
                        argThat(
                                u ->
                                        u.getUsername().equals("mehdi")
                                                && u.getPassword().equals("hashedPassword")));
        // The JWT must not leak into the response body: AuthResponse carries the
        // user's identity only, the token travels in the HttpOnly cookie.
        verify(httpServletResponse).addHeader(eq("Set-Cookie"), contains("jwt="));
    }

    @Test
    void register_shouldThrowException_whenUsernameAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(true);
        when(messageService.get("error.username.taken"))
                .thenReturn("This username is already taken");

        // WHEN & THEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.register(registerRequest, httpServletResponse));
        assertEquals("This username is already taken", ex.getMessage());
        verify(messageService).get("error.username.taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(true);
        when(messageService.get("error.email.taken")).thenReturn("This email is already in use");

        // WHEN & THEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.register(registerRequest, httpServletResponse));
        assertEquals("This email is already in use", ex.getMessage());
        verify(messageService).get("error.email.taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldNotCheckEmailUniqueness_whenUsernameAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(true);
        when(messageService.get("error.username.taken"))
                .thenReturn("This username is already taken");

        // WHEN
        assertThrows(
                IllegalArgumentException.class,
                () -> userService.register(registerRequest, httpServletResponse));

        // THEN — email check should never be called if username is already taken
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void login_shouldAuthenticate_whenLoginWithUsername() {
        // GIVEN
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class)))
                .thenReturn(mockRefreshToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.login(loginRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository).findByUsername("mehdi");
        verify(userRepository, never()).findByEmail("mehdi");
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldAuthenticate_whenLoginWithEmail() {
        // GIVEN
        loginRequest.setIdentifier("mehdi@test.com");
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class)))
                .thenReturn(mockRefreshToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi@test.com"))
                .thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("mehdi@test.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.login(loginRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository).findByUsername("mehdi@test.com");
        verify(userRepository).findByEmail("mehdi@test.com");
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("mehdi")).thenReturn(java.util.Optional.empty());
        when(messageService.get("error.user.not.found")).thenReturn("User not found");

        // WHEN & THEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.login(loginRequest, httpServletResponse));
        assertEquals("User not found", ex.getMessage());
        verify(messageService).get("error.user.not.found");
        verify(userRepository).findByUsername("mehdi");
        verify(userRepository).findByEmail("mehdi");
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void login_shouldThrow_whenBadCredentials() {
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        BadCredentialsException ex =
                assertThrows(
                        BadCredentialsException.class,
                        () -> userService.login(loginRequest, httpServletResponse));

        assertEquals("Bad credentials", ex.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }

    @Test
    void getUserProfile_shouldReturnUserProfile_whenAuthenticated() {
        // GIVEN
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        user.setCreatedAt(createdAt);
        when(securityUtils.getCurrentUser()).thenReturn(user);

        // WHEN
        UserResponse response = userService.getUserProfile();

        // THEN
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        assertEquals("ROLE_USER", response.getRole());
        assertEquals(createdAt, response.getCreatedAt());
        verify(securityUtils).getCurrentUser();
    }

    @Test
    void changePassword_shouldChangePassword_whenCurrentPasswordIsCorrect() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("OldPassword@2026", "hashedPassword")).thenReturn(true);
        when(passwordEncoder.matches("NewPassword@2026", "hashedPassword")).thenReturn(false);
        when(passwordEncoder.encode("NewPassword@2026")).thenReturn("newHashedPassword");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("OldPassword@2026");
        request.setNewPassword("NewPassword@2026");

        // WHEN
        userService.changePassword(request);

        // THEN
        verify(securityUtils).getCurrentUser();
        verify(passwordEncoder).matches("OldPassword@2026", "hashedPassword");
        verify(passwordEncoder).matches("NewPassword@2026", "hashedPassword");
        verify(passwordEncoder).encode("NewPassword@2026");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("newHashedPassword")));
        verify(refreshTokenService).revokeAllUserTokens(user);
        verify(auditService).logPasswordChange("mehdi");
    }

    @Test
    void changePassword_shouldThrow_whenCurrentPasswordIsIncorrect() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("WrongPassword@2026", "hashedPassword")).thenReturn(false);
        when(messageService.get("error.password.incorrect"))
                .thenReturn("Current password is incorrect");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("WrongPassword@2026");
        request.setNewPassword("NewPassword@2026");

        // WHEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class, () -> userService.changePassword(request));

        // THEN
        assertEquals("Current password is incorrect", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(passwordEncoder).matches("WrongPassword@2026", "hashedPassword");
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    void changePassword_shouldThrow_whenNewPasswordIsSameAsCurrent() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("SamePassword@2026", "hashedPassword")).thenReturn(true);
        when(messageService.get("error.password.same"))
                .thenReturn("New password must be different from the current password");

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("SamePassword@2026");
        request.setNewPassword("SamePassword@2026");

        // WHEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class, () -> userService.changePassword(request));

        // THEN
        assertEquals("New password must be different from the current password", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(passwordEncoder, times(2)).matches("SamePassword@2026", "hashedPassword");
        verify(userRepository, never()).save(any());
        verify(refreshTokenService, never()).revokeAllUserTokens(any());
    }

    @Test
    void updateProfile_shouldUpdateAndReturnProfile_whenDataIsValid() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(sanitizationService.sanitizeAndLog(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByUsername("mehdi_updated")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi.updated@test.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("mehdi_updated");
        request.setEmail("mehdi.updated@test.com");

        // WHEN
        UserResponse response = userService.updateProfile(request);

        // THEN
        assertNotNull(response);
        assertEquals("mehdi_updated", response.getUsername());
        assertEquals("mehdi.updated@test.com", response.getEmail());
        verify(securityUtils).getCurrentUser();
        verify(userRepository).existsByUsername("mehdi_updated");
        verify(userRepository).existsByEmail("mehdi.updated@test.com");
        verify(userRepository)
                .save(
                        argThat(
                                u ->
                                        u.getUsername().equals("mehdi_updated")
                                                && u.getEmail().equals("mehdi.updated@test.com")));
        verify(auditService).logProfileUpdate("mehdi_updated");
    }

    @Test
    void updateProfile_shouldUpdateAndReturnProfile_whenSameUsernameAndEmail() {
        // GIVEN — user keeps the same username and email
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(sanitizationService.sanitizeAndLog(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");

        // WHEN
        UserResponse response = userService.updateProfile(request);

        // THEN
        assertNotNull(response);
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository, never()).existsByUsername(any());
        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void updateProfile_shouldThrow_whenUsernameAlreadyTakenByAnotherAccount() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(sanitizationService.sanitizeAndLog(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByUsername("other_user")).thenReturn(true);
        when(messageService.get("error.username.taken.other"))
                .thenReturn("This username is already taken by another account");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("other_user");
        request.setEmail("mehdi@test.com");

        // WHEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class, () -> userService.updateProfile(request));

        // THEN
        assertEquals("This username is already taken by another account", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(auditService, never()).logProfileUpdate(any());
    }

    @Test
    void updateProfile_shouldThrow_whenEmailAlreadyTakenByAnotherAccount() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(sanitizationService.sanitizeAndLog(any(), any(), any()))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.existsByUsername("mehdi_updated")).thenReturn(false);
        when(userRepository.existsByEmail("other@test.com")).thenReturn(true);
        when(messageService.get("error.email.taken.other"))
                .thenReturn("This email is already in use by another account");

        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setUsername("mehdi_updated");
        request.setEmail("other@test.com");

        // WHEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class, () -> userService.updateProfile(request));

        // THEN
        assertEquals("This email is already in use by another account", ex.getMessage());
        verify(userRepository, never()).save(any());
        verify(auditService, never()).logProfileUpdate(any());
    }

    @Test
    void deleteAccount_shouldDeleteAccount_whenPasswordIsCorrect() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("MyPassword@2026", "hashedPassword")).thenReturn(true);

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("MyPassword@2026");

        // WHEN
        userService.deleteAccount(request, httpServletResponse);

        // THEN
        verify(securityUtils).getCurrentUser();
        verify(passwordEncoder).matches("MyPassword@2026", "hashedPassword");
        verify(auditService).logAccountDeletion("mehdi");
        verify(userRepository).delete(user);
    }

    @Test
    void deleteAccount_shouldThrow_whenPasswordIsIncorrect() {
        // GIVEN
        when(securityUtils.getCurrentUser()).thenReturn(user);
        when(passwordEncoder.matches("WrongPassword@2026", "hashedPassword")).thenReturn(false);
        when(messageService.get("error.account.deletion.invalid.password"))
                .thenReturn("Password is incorrect — account deletion cancelled");

        DeleteAccountRequest request = new DeleteAccountRequest();
        request.setPassword("WrongPassword@2026");

        // WHEN
        IllegalArgumentException ex =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> userService.deleteAccount(request, httpServletResponse));

        // THEN
        assertEquals("Password is incorrect — account deletion cancelled", ex.getMessage());
        verify(securityUtils).getCurrentUser();
        verify(passwordEncoder).matches("WrongPassword@2026", "hashedPassword");
        verify(auditService, never()).logAccountDeletion(any());
        verify(userRepository, never()).delete(any());
    }
}
