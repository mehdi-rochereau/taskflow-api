package com.mehdi.taskflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SecurityConfig;
import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import com.mehdi.taskflow.user.UserService;
import com.mehdi.taskflow.user.dto.AuthResponse;
import com.mehdi.taskflow.user.dto.LoginRequest;
import com.mehdi.taskflow.user.dto.RegisterRequest;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private UserDetailsServiceImpl userDetailsService;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private JwtFilter jwtFilter;

    @MockitoBean
    private MessageService messageService;

    @BeforeEach
    void setUp() throws ServletException, IOException {

        Locale.setDefault(java.util.Locale.ENGLISH);

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(jwtFilter).doFilter(any(), any(), any());
    }

    @Test
    void register_shouldReturn201_whenRequestIsValid() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("fake-token", "mehdi", "mehdi@test.com");
        when(userService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("fake-token"))
                .andExpect(jsonPath("$.username").value("mehdi"))
                .andExpect(jsonPath("$.email").value("mehdi@test.com"));
    }

    @Test
    void register_shouldReturn400_whenUsernameIsBlank() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username",
                        Matchers.containsInAnyOrder(
                                "Username is required",
                                "Username must be between 3 and 50 characters"
                        )));
    }

    @Test
    void register_shouldReturn400_whenUsernameIsTooShort() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("me");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.username").isArray())
                .andExpect(jsonPath("$.errors.username",
                        Matchers.contains(
                                "Username must be between 3 and 50 characters"
                        )));
    }

    @Test
    void register_shouldReturn400_whenUsernameIsTooLong() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("m".repeat(51));
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.username").isArray())
                .andExpect(jsonPath("$.errors.username",
                        Matchers.contains(
                                "Username must be between 3 and 50 characters"
                        )));
    }

    @Test
    void register_shouldReturn400_whenEmailIsInvalid() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("not-an-email");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.email",
                        Matchers.contains(
                                "Invalid email address"
                        )));
    }

    @Test
    void register_shouldReturn400_whenEmailIsBlank() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.email",
                        Matchers.contains(
                                "Email is required"
                        )));
    }

    @Test
    void register_shouldReturn400_whenPasswordIsTooShort() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(jsonPath("$.errors.password",
                        Matchers.contains(
                                "Password must be at least 8 characters")));
    }

    @Test
    void register_shouldReturn400_whenPasswordIsBlank() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(jsonPath("$.errors.password",
                        Matchers.containsInAnyOrder(
                                "Password is required",
                                "Password must be at least 8 characters")));
    }

    @Test
    void register_shouldReturn400_whenUsernameAlreadyExists() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("This username is already taken"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("This username is already taken"));
    }

    @Test
    void register_shouldReturn500_whenUnexpectedErrorOccurs() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");
        when(messageService.get("error.unexpected")).thenReturn("An unexpected error occurred");
        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("unexpected"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"));
    }

    @Test
    void login_shouldReturn200_whenCredentialsWithUsernameAreValid() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("mehdi");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("fake-token", "mehdi", "mehdi@test.com");
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-token"))
                .andExpect(jsonPath("$.username").value("mehdi"))
                .andExpect(jsonPath("$.email").value("mehdi@test.com"));
    }

    @Test
    void login_shouldReturn200_whenCredentialsWithEmailAreValid() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("mehdi@test.com");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("fake-token", "mehdi", "mehdi@test.com");
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-token"))
                .andExpect(jsonPath("$.username").value("mehdi"))
                .andExpect(jsonPath("$.email").value("mehdi@test.com"));
    }

    @Test
    void login_shouldReturn400_whenIdentifierIsBlank() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.identifier").exists())
                .andExpect(jsonPath("$.errors.identifier").isArray())
                .andExpect(jsonPath("$.errors.identifier",
                        Matchers.contains(
                                "Username or email is required")));
    }

    @Test
    void login_shouldReturn400_whenPasswordIsBlank() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("mehdi");
        request.setPassword("");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(jsonPath("$.errors.password",
                        Matchers.contains(
                                "Password is required"
                        )));
    }

    @Test
    void login_shouldReturn400_whenIdentifierAndPasswordAreBlanks() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("");
        request.setPassword("");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.identifier").exists());
    }

    @Test
    void login_shouldReturn401_whenBadCredentials() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setIdentifier("invalid-username");
        request.setPassword("invalid-password");
        when(messageService.get("error.bad.credentials")).thenReturn("Invalid username or password");
        when(userService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));


        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid username or password"))
                .andExpect(jsonPath("$.timestamp").exists());
    }
}