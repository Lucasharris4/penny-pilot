package com.pennypilot.api.service;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.config.CategoryProperties;
import com.pennypilot.api.util.FixedClock;
import com.pennypilot.api.dto.auth.LoginRequest;
import com.pennypilot.api.dto.auth.LoginResponse;
import com.pennypilot.api.dto.auth.RegisterRequest;
import com.pennypilot.api.dto.auth.UserResponse;
import com.pennypilot.api.entity.Category;
import com.pennypilot.api.entity.User;
import com.pennypilot.api.repository.CategoryRepository;
import com.pennypilot.api.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthServiceTest {

    private UserRepository userRepository;
    private CategoryRepository categoryRepository;
    private PasswordEncoder passwordEncoder;
    private AuthService authService;
    private JwtService jwtService;
    private CategoryService categoryService;
    private LoginSyncService loginSyncService;
    private FixedClock clock;

    private static final Instant FIXED_TIME = Instant.now();
    private static final AuthProperties AUTH_PROPS =
            new AuthProperties(8, "test-secret-that-is-at-least-32-bytes-long!", 86400000);
    private static final CategoryProperties CATEGORY_PROPS = new CategoryProperties(true, List.of(
            new CategoryProperties.DefaultCategory("Groceries", "cart", "#4CAF50"),
            new CategoryProperties.DefaultCategory("Subscriptions", "refresh", "#607D8B")
    ));

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        categoryRepository = mock(CategoryRepository.class);
        loginSyncService = mock(LoginSyncService.class);
        passwordEncoder = new BCryptPasswordEncoder();
        clock = new FixedClock(FIXED_TIME);
        jwtService = new JwtService(AUTH_PROPS, clock);
        categoryService = new CategoryService(categoryRepository, CATEGORY_PROPS);
        authService = new AuthService(userRepository, passwordEncoder, AUTH_PROPS, jwtService,
                categoryService, loginSyncService);
    }

    @Test
    void register_success() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(FIXED_TIME);
            return user;
        });

        UserResponse response = authService.register(new RegisterRequest("user@example.com", "password123"));

        assertEquals(1L, response.id());
        assertEquals("user@example.com", response.email());
        assertEquals(FIXED_TIME, response.createdAt());

        // Verify default categories were seeded via real CategoryService
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Category>> categoryCaptor = ArgumentCaptor.forClass(List.class);
        verify(categoryRepository).saveAll(categoryCaptor.capture());
        List<Category> seededCategories = categoryCaptor.getValue();
        assertEquals(2, seededCategories.size());
        assertEquals("Groceries", seededCategories.get(0).getName());
        assertEquals(1L, seededCategories.get(0).getUserId());
    }

    @Test
    void register_hashesPassword() {
        when(userRepository.existsByEmail(any())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(FIXED_TIME);
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

    @Test
    void login_success() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));
        user.setCreatedAt(FIXED_TIME);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        LoginResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

        assertNotNull(response.token());
        assertTrue(jwtService.isValid(response.token()));
        assertEquals(1L, jwtService.getUserIdFromToken(response.token()));
    }

    @Test
    void login_wrongPassword_throws() {
        User user = new User();
        user.setId(1L);
        user.setEmail("user@example.com");
        user.setPasswordHash(passwordEncoder.encode("password123"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        assertThrows(AuthService.InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("user@example.com", "wrongpassword")));
    }

    @Test
    void login_unknownEmail_throws() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThrows(AuthService.InvalidCredentialsException.class,
                () -> authService.login(new LoginRequest("unknown@example.com", "password123")));
    }
}
