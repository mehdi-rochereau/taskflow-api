package com.mehdi.taskflow.security;

import com.mehdi.taskflow.config.MessageService;
import com.mehdi.taskflow.user.User;
import com.mehdi.taskflow.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setUsername("mehdi");
        user.setEmail("mehdi@test.com");
        user.setPassword("hashedPassword");
        user.setRole("ROLE_USER");
    }

    @Test
    void loadUserByUsername_shouldReturnUser_whenFoundByUsername() {
        // GIVEN
        when(userRepository.findByUsername("mehdi")).thenReturn(Optional.of(user));

        // WHEN
        UserDetails result = userDetailsService.loadUserByUsername("mehdi");

        // THEN
        assertNotNull(result);
        assertEquals("mehdi", result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        verify(userRepository).findByUsername("mehdi");
        verify(userRepository, never()).findByEmail(any());
        verify(messageService, never()).get(any());
    }

    @Test
    void loadUserByUsername_shouldReturnUser_whenFoundByEmail() {
        // GIVEN
        when(userRepository.findByUsername("mehdi@test.com")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("mehdi@test.com")).thenReturn(Optional.of(user));

        // WHEN
        UserDetails result = userDetailsService.loadUserByUsername("mehdi@test.com");

        // THEN
        assertNotNull(result);
        assertEquals("mehdi", result.getUsername());
        assertEquals("hashedPassword", result.getPassword());
        verify(userRepository).findByUsername("mehdi@test.com");
        verify(userRepository).findByEmail("mehdi@test.com");
        verify(messageService, never()).get(any());
    }

    @Test
    void loadUserByUsername_shouldThrow_whenUserNotFound() {
        // GIVEN
        when(userRepository.findByUsername("unknown")).thenReturn(Optional.empty());
        when(userRepository.findByEmail("unknown")).thenReturn(Optional.empty());
        when(messageService.get("error.identifier.not.found", "unknown"))
                .thenReturn("No user found with identifier: unknown");

        // WHEN
        UsernameNotFoundException ex = assertThrows(UsernameNotFoundException.class,
                () -> userDetailsService.loadUserByUsername("unknown"));

        // THEN
        assertEquals("No user found with identifier: unknown", ex.getMessage());
        verify(userRepository).findByUsername("unknown");
        verify(userRepository).findByEmail("unknown");
        verify(messageService).get("error.identifier.not.found", "unknown");
    }
}