package com.mehdi.taskflow.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

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

    @BeforeEach
    void setUp() throws ServletException, IOException {
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
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
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_shouldReturn400_whenUsernameAlreadyExists() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new IllegalArgumentException("Ce nom d'utilisateur est déjà pris"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ce nom d'utilisateur est déjà pris"));
    }

    @Test
    void register_shouldReturn500_whenUnexpectedErrorOccurs() throws Exception {
        // GIVEN
        RegisterRequest request = new RegisterRequest();
        request.setUsername("mehdi");
        request.setEmail("mehdi@test.com");
        request.setPassword("password123");

        when(userService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Erreur inattendue"));

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value("Une erreur inattendue s'est produite"));
    }

    @Test
    void login_shouldReturn200_whenCredentialsAreValid() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setUsername("mehdi");
        request.setPassword("password123");

        AuthResponse authResponse = new AuthResponse("fake-token", "mehdi", "mehdi@test.com");
        when(userService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("fake-token"))
                .andExpect(jsonPath("$.username").value("mehdi"));
    }

    @Test
    void login_shouldReturn400_whenUsernameIsBlank() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setUsername("");
        request.setPassword("password123");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_shouldReturn400_whenPasswordIsBlank() throws Exception {
        // GIVEN
        LoginRequest request = new LoginRequest();
        request.setUsername("mehdi");
        request.setPassword("");

        // WHEN & THEN
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }
}