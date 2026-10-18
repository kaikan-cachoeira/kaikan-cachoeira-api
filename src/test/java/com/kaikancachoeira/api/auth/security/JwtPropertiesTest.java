package com.kaikancachoeira.api.auth.security;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class JwtPropertiesTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void configurarValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void fecharValidator() {
        factory.close();
    }

    private JwtProperties propriedadesValidas() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(
                "segredo-de-teste-com-pelo-menos-32-bytes"
        );
        properties.setIssuer("kaikan-cachoeira-api-test");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(7));
        return properties;
    }

    @Test
    void deveAceitarConfiguracaoValida() {
        JwtProperties properties = propriedadesValidas();

        assertTrue(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarSegredoComMenosDe32Bytes() {
        JwtProperties properties = propriedadesValidas();
        properties.setSecret("segredo-curto");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarSegredoNulo() {
        JwtProperties properties = propriedadesValidas();
        properties.setSecret(null);

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarSegredoEmBranco() {
        JwtProperties properties = propriedadesValidas();
        properties.setSecret("   ");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarIssuerEmBranco() {
        JwtProperties properties = propriedadesValidas();
        properties.setIssuer(" ");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarAccessTokenTtlZero() {
        JwtProperties properties = propriedadesValidas();
        properties.setAccessTokenTtl(Duration.ZERO);

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarAccessTokenTtlNegativo() {
        JwtProperties properties = propriedadesValidas();
        properties.setAccessTokenTtl(Duration.ofMinutes(-1));

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarRefreshTokenTtlZero() {
        JwtProperties properties = propriedadesValidas();
        properties.setRefreshTokenTtl(Duration.ZERO);

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarRefreshTokenTtlNegativo() {
        JwtProperties properties = propriedadesValidas();
        properties.setRefreshTokenTtl(Duration.ofDays(-1));

        assertFalse(validator.validate(properties).isEmpty());
    }
}