package com.pennypilot.api.service;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.dto.RegisterRequest;
import com.pennypilot.api.dto.UserResponse;
import com.pennypilot.api.entity.User;
import com.pennypilot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        passwordEncoder = new BCryptPasswordEncoder();
        AuthProperties authProperties = new AuthProperties(8, "test-secret", 86400000);
        authService = new AuthService(userRepository, passwordEncoder, authProperties);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(java.time.Instant.now());
            return user;
        });

        UserResponse response = authService.register(new RegisterRequest("user@example.com", "password123"));

        assertEquals(1L, response.id());
        assertEquals("user@example.com", response.email());
        assertNotNull(response.createdAt());
    }

    @Test
    void register_hashesPassword() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(java.time.Instant.now());
            return user;
        });

        authService.register(new RegisterRequest("user@example.com", "password123"));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertNotEquals("password123", saved.getPasswordHash());
        assertTrue(passwordEncoder.matches("password123", saved.getPasswordHash()));
    }

    @Test
    void register_duplicateEmail_throws() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThrows(AuthService.EmailAlreadyExistsException.class,
                () -> authService.register(new RegisterRequest("user@example.com", "password123")));
    }

    @Test
    void register_shortPassword_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> authService.register(new RegisterRequest("user@example.com", "short")));
    }
}
