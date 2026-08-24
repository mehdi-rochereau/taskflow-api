package com.mehdi.taskflow.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mehdi.taskflow.config.AuditService;
import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.config.SecurityConfig;
import com.mehdi.taskflow.exception.PasswordVerificationException;
import com.mehdi.taskflow.security.JwtFilter;
import com.mehdi.taskflow.security.JwtService;
import com.mehdi.taskflow.security.RateLimitFilter;
import com.mehdi.taskflow.security.UserDetailsServiceImpl;
import com.mehdi.taskflow.user.dto.ChangePasswordRequest;
import com.mehdi.taskflow.user.dto.DeleteAccountRequest;
import com.mehdi.taskflow.user.dto.UpdateProfileRequest;
import com.mehdi.taskflow.user.dto.UserResponse;
import jakarta.servlet.FilterChain;
import java.time.LocalDateTime;
import java.util.Locale;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@WithMockUser(username = "mehdi")
public class UserControllerTest {

    @Autowired private MockMvc mockMvc;

    @Autowired private ObjectMapper objectMapper;

    @MockitoBean private UserService userService;

    @MockitoBean private JwtService jwtService;

    @MockitoBean private JwtFilter jwtFilter;

    @MockitoBean private RateLimitFilter rateLimitFilter;

    @MockitoBean private UserDetailsServiceImpl userDetailsService;

    @MockitoBean private PasswordEncoder passwordEncoder;

    @MockitoBean private MessageService messageService;

    @MockitoBean private AuditService auditService;

    private UserResponse userResponse;
    private ChangePasswordRequest changePasswordRequest;
    private UpdateProfileRequest updateProfileRequest;
    private DeleteAccountRequest deleteAccountRequest;

    @BeforeEach
    void setUp() throws Exception {

        Locale.setDefault(Locale.ENGLISH);

        userResponse =
                new UserResponse(
                        1L, "mehdi", "mehdi@example.com", "ROLE_USER", LocalDateTime.now());

        changePasswordRequest = new ChangePasswordRequest();
        changePasswordRequest.setCurrentPassword("OldPassword1!");
        changePasswordRequest.setNewPassword("NewPassword1!");

        updateProfileRequest = new UpdateProfileRequest();
        updateProfileRequest.setUsername("mehdi");
        updateProfileRequest.setEmail("mehdi@example.com");

        deleteAccountRequest = new DeleteAccountRequest();
        deleteAccountRequest.setPassword("OldPassword1!");

        // Both filters are mocked, so the chain would stop dead without this:
        // the request must be handed on to the next filter for the controller
        // to be reached at all.
        doAnswer(
                        invocation -> {
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                            return null;
                        })
                .when(jwtFilter)
                .doFilter(any(), any(), any());

        doAnswer(
                        invocation -> {
                            FilterChain chain = invocation.getArgument(2);
                            chain.doFilter(invocation.getArgument(0), invocation.getArgument(1));
                            return null;
                        })
                .when(rateLimitFilter)
                .doFilter(any(), any(), any());
    }

    // ── getProfile ───────────────────────────────────────────────────

    @Test
    void getProfile_shouldReturn200_withTheAuthenticatedUserProfile() throws Exception {
        // GIVEN
        when(userService.getUserProfile()).thenReturn(userResponse);

        // WHEN & THEN
        // The password is never part of UserResponse; asserting its absence
        // guards against a future field being added to the DTO by accident.
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("mehdi"))
                .andExpect(jsonPath("$.email").value("mehdi@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    @WithAnonymousUser
    void getProfile_shouldReturn401_whenUnauthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    // ── changePassword ───────────────────────────────────────────────

    @Test
    void changePassword_shouldReturn204_whenRequestIsValid() throws Exception {
        // WHEN & THEN
        mockMvc.perform(
                        post("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isNoContent());

        // THEN
        verify(userService).changePassword(any(ChangePasswordRequest.class));
    }

    @Test
    void changePassword_shouldReturn422_whenCurrentPasswordIsIncorrect() throws Exception {
        // GIVEN
        doThrow(new PasswordVerificationException("Current password is incorrect"))
                .when(userService)
                .changePassword(any(ChangePasswordRequest.class));

        // WHEN & THEN
        mockMvc.perform(
                        post("/api/users/me/password")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(changePasswordRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value("Current password is incorrect"));
    }

    @Test
    void deleteAccount_shouldReturn422_whenPasswordIsIncorrect() throws Exception {
        // GIVEN
        doThrow(
                        new PasswordVerificationException(
                                "Password is incorrect — account deletion cancelled"))
                .when(userService)
                .deleteAccount(any(DeleteAccountRequest.class), any());

        // WHEN & THEN
        mockMvc.perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deleteAccountRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(
                        jsonPath("$.message")
                                .value("Password is incorrect — account deletion cancelled"));
    }

    @Test
    @WithAnonymousUser
    void changePassword_shouldReturn401_whenUnauthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(post("/api/users/me/password"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    // ── updateProfile ────────────────────────────────────────────────

    @Test
    void updateProfile_shouldReturn200_withTheUpdatedProfile() throws Exception {
        // GIVEN
        when(userService.updateProfile(any(UpdateProfileRequest.class))).thenReturn(userResponse);

        // WHEN & THEN
        mockMvc.perform(
                        put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.username").value("mehdi"))
                .andExpect(jsonPath("$.email").value("mehdi@example.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void updateProfile_shouldReturn400_whenUsernameIsTaken() throws Exception {
        // GIVEN
        when(userService.updateProfile(any(UpdateProfileRequest.class)))
                .thenThrow(
                        new IllegalArgumentException(
                                "This username is already taken by another account"));

        // WHEN & THEN
        mockMvc.perform(
                        put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("This username is already taken by another account"));
    }

    @Test
    void updateProfile_shouldReturn400_whenEmailIsInvalid() throws Exception {
        // GIVEN
        // Rejected by bean validation before the service is ever reached, so
        // the error surfaces per field rather than as a single message.
        updateProfileRequest.setEmail("not-an-email");

        // WHEN & THEN
        mockMvc.perform(
                        put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.email").isArray())
                .andExpect(jsonPath("$.errors.email", Matchers.contains("Invalid email address")));
    }

    @Test
    void updateProfile_shouldReturn400_whenUsernameIsBlank() throws Exception {
        // GIVEN
        updateProfileRequest.setUsername("");

        // WHEN & THEN
        mockMvc.perform(
                        put("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(updateProfileRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.username").exists())
                .andExpect(jsonPath("$.errors.username").isArray())
                // A blank username trips both @NotBlank and @Size, so the array
                // carries two entries and the order is not guaranteed.
                .andExpect(
                        jsonPath(
                                "$.errors.username",
                                Matchers.containsInAnyOrder(
                                        "Username is required",
                                        "Username must be between 3 and 50 characters")));
    }

    @Test
    @WithAnonymousUser
    void updateProfile_shouldReturn401_whenUnauthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(put("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    // ── deleteAccount ────────────────────────────────────────────────

    @Test
    void deleteAccount_shouldReturn204_whenPasswordIsConfirmed() throws Exception {
        // WHEN & THEN
        mockMvc.perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deleteAccountRequest)))
                .andExpect(status().isNoContent());

        // THEN
        verify(userService).deleteAccount(any(DeleteAccountRequest.class), any());
    }

    @Test
    void deleteAccount_shouldReturn400_whenPasswordIsIncorrect() throws Exception {
        // GIVEN
        // Same reservation as changePassword above: issue #58 may move this to
        // 401. The OpenAPI description on UserControllerApi reads
        // "Validation failed or password incorrect" and would move with it.
        doThrow(new IllegalArgumentException("Password is incorrect — account deletion cancelled"))
                .when(userService)
                .deleteAccount(any(DeleteAccountRequest.class), any());

        // WHEN & THEN
        mockMvc.perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deleteAccountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.message")
                                .value("Password is incorrect — account deletion cancelled"));
    }

    @Test
    void deleteAccount_shouldReturn400_whenPasswordIsBlank() throws Exception {
        // GIVEN
        deleteAccountRequest.setPassword("");

        // WHEN & THEN
        mockMvc.perform(
                        delete("/api/users/me")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(deleteAccountRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.password").isArray())
                .andExpect(
                        jsonPath(
                                "$.errors.password",
                                Matchers.contains("Current password is required")));
    }

    @Test
    @WithAnonymousUser
    void deleteAccount_shouldReturn401_whenUnauthenticated() throws Exception {
        // GIVEN
        when(messageService.get("error.authentication.required"))
                .thenReturn("Authentication required");

        // WHEN & THEN
        mockMvc.perform(delete("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }
}
