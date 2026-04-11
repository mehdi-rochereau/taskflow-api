package com.mehdi.taskflow.user;

import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
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
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.register(registerRequest);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("mehdi", response.getUsername());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenUsernameAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(true);

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(true);

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> userService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldEncodePassword() {
        // GIVEN
        when(userRepository.existsByUsername("mehdi")).thenReturn(false);
        when(userRepository.existsByEmail("mehdi@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");
        when(userRepository.save(any(User.class))).thenReturn(user);
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        userService.register(registerRequest);

        // THEN
        verify(passwordEncoder, times(1)).encode("password123");
        verify(userRepository).save(argThat(u -> u.getPassword().equals("hashedPassword")));
    }

    @Test
    void login_shouldReturnToken_whenCredentialsAreValid() {
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi")).thenReturn(java.util.Optional.of(user));
        when(jwtService.generateToken(any(User.class))).thenReturn("fake-jwt-token");

        // WHEN
        AuthResponse response = userService.login(loginRequest);

        // THEN
        assertNotNull(response);
        assertEquals("fake-jwt-token", response.getToken());
        assertEquals("mehdi", response.getUsername());
    }

    @Test
    void login_shouldThrow_whenUserNotFound() {
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(null);
        when(userRepository.findByUsername("mehdi")).thenReturn(java.util.Optional.empty());

        // WHEN & THEN
        assertThrows(IllegalArgumentException.class,
                () -> userService.login(loginRequest));
    }

    @Test
    void login_shouldThrow_whenBadCredentials() {
        // GIVEN
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("Bad credentials"));

        // WHEN & THEN
        assertThrows(org.springframework.security.authentication.BadCredentialsException.class,
                () -> userService.login(loginRequest));
        verify(userRepository, never()).findByUsername(anyString());
    }
}