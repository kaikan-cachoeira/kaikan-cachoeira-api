package com.kaikancachoeira.api.auth.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenSenhaPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurar() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fechar() {
        factory.close();
    }

    private TokenSenhaProperties comTtl(Duration ttl) {
        TokenSenhaProperties properties = new TokenSenhaProperties();
        properties.setTokenSenhaTtl(ttl);
        return properties;
    }

    @Test
    void deveAceitarTtlPositivo() {
        assertTrue(validator.validate(comTtl(Duration.ofHours(1))).isEmpty());
    }

    @Test
    void deveRejeitarTtlNulo() {
        assertFalse(validator.validate(comTtl(null)).isEmpty());
    }

    @Test
    void deveRejeitarTtlZero() {
        assertFalse(validator.validate(comTtl(Duration.ZERO)).isEmpty());
    }

    @Test
    void deveRejeitarTtlNegativo() {
        assertFalse(validator.validate(comTtl(Duration.ofMinutes(-5))).isEmpty());
    }
}