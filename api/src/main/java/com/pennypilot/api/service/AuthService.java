package com.pennypilot.api.service;

import com.pennypilot.api.config.AuthProperties;
import com.pennypilot.api.dto.LoginRequest;
import com.pennypilot.api.dto.LoginResponse;
import com.pennypilot.api.dto.RegisterRequest;
import com.pennypilot.api.dto.UserResponse;
import com.pennypilot.api.entity.User;
import com.pennypilot.api.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthProperties authProperties;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthProperties authProperties, JwtService jwtService,
                       CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
    }

    public UserResponse register(RegisterRequest request) {
        if (request.password().length() < authProperties.passwordMinLength()) {
            throw new IllegalArgumentException(
                    "Password must be at least " + authProperties.passwordMinLength() + " characters");
        }

        if (userRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException(request.email());
        }

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));

        User saved = userRepository.save(user);
        categoryService.seedDefaults(saved.getId());
        return UserResponse.from(saved);
    }

    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        return new LoginResponse(token);
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Invalid email or password");
        }
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }
}
