package com.kaikancachoeira.api.auth.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CorsPropertiesTest {

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

    private CorsProperties comOrigens(List<String> origens) {
        CorsProperties properties = new CorsProperties();
        properties.setAllowedOrigins(origens);
        return properties;
    }

    @Test
    void deveAceitarAoMenosUmaOrigem() {
        assertTrue(validator.validate(comOrigens(List.of("http://localhost:5173"))).isEmpty());
    }

    @Test
    void deveRejeitarListaNula() {
        assertFalse(validator.validate(comOrigens(null)).isEmpty());
    }

    @Test
    void deveRejeitarListaVazia() {
        assertFalse(validator.validate(comOrigens(List.of())).isEmpty());
    }
}