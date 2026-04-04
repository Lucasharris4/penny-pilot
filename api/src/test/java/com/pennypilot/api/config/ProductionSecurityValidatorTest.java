package com.pennypilot.api.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.junit.jupiter.api.Assertions.*;

class ProductionSecurityValidatorTest {

    private static final String DEFAULT_JWT = "penny-pilot-default-dev-secret-key-change-me";
    private static final String DEFAULT_ENCRYPTION = "penny-pilot-default-dev-encryption-key-change-me";
    private static final String STRONG_JWT = "super-secret-production-jwt-key-2026";
    private static final String STRONG_ENCRYPTION = "super-secret-production-encryption-key-2026";

    @Test
    void startsSuccessfully_withStrongSecrets() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(STRONG_JWT, STRONG_ENCRYPTION);
        assertDoesNotThrow(() -> validator.run(new DefaultApplicationArguments()));
    }

    @Test
    void failsStartup_withDefaultJwtSecret() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(DEFAULT_JWT, STRONG_ENCRYPTION);
        var ex = assertThrows(ProductionSecurityValidator.SecurityConfigurationException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("insecure default secrets"));
    }

    @Test
    void failsStartup_withDefaultEncryptionKey() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(STRONG_JWT, DEFAULT_ENCRYPTION);
        var ex = assertThrows(ProductionSecurityValidator.SecurityConfigurationException.class,
                () -> validator.run(new DefaultApplicationArguments()));
        assertTrue(ex.getMessage().contains("insecure default secrets"));
    }

    @Test
    void failsStartup_withBothDefaultSecrets() {
        ProductionSecurityValidator validator = new ProductionSecurityValidator(DEFAULT_JWT, DEFAULT_ENCRYPTION);
        assertThrows(ProductionSecurityValidator.SecurityConfigurationException.class,
                () -> validator.run(new DefaultApplicationArguments()));
    }
}
