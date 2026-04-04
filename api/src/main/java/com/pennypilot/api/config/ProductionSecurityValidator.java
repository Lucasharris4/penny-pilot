package com.pennypilot.api.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConditionalOnProperty(name = "app.security.validate-secrets", havingValue = "true", matchIfMissing = true)
public class ProductionSecurityValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecurityValidator.class);

    private static final String DEFAULT_JWT_SECRET = "penny-pilot-default-dev-secret-key-change-me";
    private static final String DEFAULT_ENCRYPTION_KEY = "penny-pilot-default-dev-encryption-key-change-me";

    private final String jwtSecret;
    private final String encryptionKey;

    public ProductionSecurityValidator(
            @Value("${app.auth.jwt-secret}") String jwtSecret,
            @Value("${app.credentials.encryption-key}") String encryptionKey) {
        this.jwtSecret = jwtSecret;
        this.encryptionKey = encryptionKey;
    }

    @Override
    public void run(ApplicationArguments args) {
        List<String> violations = new ArrayList<>();

        if (DEFAULT_JWT_SECRET.equals(jwtSecret)) {
            violations.add("JWT_SECRET is set to the default development value. " +
                    "Set a strong, unique JWT_SECRET environment variable for production.");
        }

        if (DEFAULT_ENCRYPTION_KEY.equals(encryptionKey)) {
            violations.add("CREDENTIAL_ENCRYPTION_KEY is set to the default development value. " +
                    "Set a strong, unique CREDENTIAL_ENCRYPTION_KEY environment variable for production.");
        }

        if (!violations.isEmpty()) {
            for (String v : violations) {
                log.error("SECURITY: {}", v);
            }
            throw new SecurityConfigurationException(
                    "Application startup aborted: insecure default secrets detected in production. " +
                    "See error logs above for details.");
        }

        log.info("Production security validation passed");
    }

    public static class SecurityConfigurationException extends RuntimeException {
        public SecurityConfigurationException(String message) {
            super(message);
        }
    }
}
