package com.pennypilot.api.service;

import com.pennypilot.api.config.AuthProperties;
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

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       AuthProperties authProperties) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authProperties = authProperties;
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
        return UserResponse.from(saved);
    }

    public static class EmailAlreadyExistsException extends RuntimeException {
        public EmailAlreadyExistsException(String email) {
            super("Email already registered: " + email);
        }
    }
}
