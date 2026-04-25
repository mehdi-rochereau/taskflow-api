package com.mehdi.taskflow.user;

import com.mehdi.taskflow.auth.RefreshToken;
import com.mehdi.taskflow.auth.RefreshTokenService;
import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import jakarta.servlet.http.HttpServletResponse;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private HttpServletResponse httpServletResponse;

    @Mock
    private MessageService messageService;

    @Mock
    private AuditService auditService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @InjectMocks
    private UserService userService;

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
    void register_shouldCreateUserAndReturnToken() {
        // GIVEN
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class))).thenReturn(mockRefreshToken);
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.register(registerRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository).existsByUsername("mehdi");
        verify(userRepository).existsByEmail("mehdi@test.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(argThat(u ->
                u.getUsername().equals("mehdi")
                        && u.getEmail().equals("mehdi@test.com")
                        && u.getPassword().equals("hashedPassword")
                        && u.getRole().equals("ROLE_USER")
        ));
        verify(jwtService).generateToken(argThat(u ->
                u.getUsername().equals("mehdi")
                        && u.getPassword().equals("hashedPassword")));
    }

    @Test
    void register_shouldThrowException_whenUsernameAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(true);
        when(messageService.get("error.username.taken")).thenReturn("This username is already taken");

        // WHEN & THEN
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> userService.register(registerRequest, httpServletResponse));
        assertEquals("This email is already in use", ex.getMessage());
        verify(messageService).get("error.email.taken");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldNotCheckEmailUniqueness_whenUsernameAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(true);
        when(messageService.get("error.username.taken")).thenReturn("This username is already taken");

        // WHEN
        assertThrows(IllegalArgumentException.class,
                () -> userService.register(registerRequest, httpServletResponse));

        // THEN — email check should never be called if username is already taken
        verify(userRepository, never()).existsByEmail(anyString());
    }

    @Test
    void login_shouldReturnToken_whenLoginWithUsername() {
        // GIVEN
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class))).thenReturn(mockRefreshToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.login(loginRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("mehdi", response.getUsername());
        assertEquals("mehdi@test.com", response.getEmail());
        verify(userRepository).findByUsername("mehdi");
        verify(userRepository, never()).findByEmail("mehdi");
        verify(jwtService).generateToken(user);
    }

    @Test
    void login_shouldReturnToken_whenLoginWithEmail() {
        // GIVEN
        loginRequest.setIdentifier("mehdi@test.com");
        RefreshToken mockRefreshToken = new RefreshToken();
        mockRefreshToken.setToken("fake-refresh-token");
        when(refreshTokenService.generateRefreshToken(any(User.class))).thenReturn(mockRefreshToken);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi@test.com")).thenReturn(java.util.Optional.empty());
        when(userRepository.findByEmail("mehdi@test.com")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.login(loginRequest, httpServletResponse);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
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
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
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
        BadCredentialsException ex = assertThrows(BadCredentialsException.class,
                () -> userService.login(loginRequest, httpServletResponse));

        assertEquals("Bad credentials", ex.getMessage());
        verify(userRepository, never()).findByUsername(anyString());
        verify(userRepository, never()).findByEmail(anyString());
    }
}