package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.exception.ResourceNotFoundException;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private SecurityUtils securityUtils;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("mehdi");
        user.setEmail("mehdi@test.com");
        user.setPassword("hashedPassword");
        user.setRole("ROLE_USER");

        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken("mehdi", null, Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentUser_shouldReturnUser_whenAuthenticated() {
        // GIVEN
        when(userRepository.findByUsername("mehdi")).thenReturn(Optional.of(user));

        // WHEN
        User result = securityUtils.getCurrentUser();

        // THEN
        assertNotNull(result);
        assertEquals("mehdi", result.getUsername());
        assertEquals("mehdi@test.com", result.getEmail());
        assertEquals(1L, result.getId());
        verify(userRepository).findByUsername("mehdi");
        verify(messageService, never()).get(any());
    }

    @Test
    void getCurrentUser_shouldThrow_whenUserNotFound() {
        // GIVEN
        when(userRepository.findByUsername("mehdi")).thenReturn(Optional.empty());
        when(messageService.get("error.user.not.found")).thenReturn("User not found");

        // WHEN
        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> securityUtils.getCurrentUser());

        // THEN
        assertEquals("User not found", ex.getMessage());
        verify(userRepository).findByUsername("mehdi");
        verify(messageService).get("error.user.not.found");
    }
}