package com.kaikancachoeira.api.email.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmailPropertiesTest {

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

    private EmailProperties valida() {
        EmailProperties properties = new EmailProperties();
        properties.setProvider("smtp");
        properties.setFrontUrl("http://localhost:5173");
        properties.setRemetente("no-reply@kaikan.test");
        return properties;
    }

    @ParameterizedTest
    @ValueSource(strings = {"smtp", "resend"})
    void deveAceitarProvidersSuportados(String provider) {
        EmailProperties properties = valida();
        properties.setProvider(provider);

        assertTrue(validator.validate(properties).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"gmail", "SMTP", "smtp "})
    void deveRejeitarProviderNaoSuportado(String provider) {
        EmailProperties properties = valida();
        properties.setProvider(provider);

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarProviderEmBranco() {
        EmailProperties properties = valida();
        properties.setProvider(" ");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarFrontUrlEmBranco() {
        EmailProperties properties = valida();
        properties.setFrontUrl("");

        assertFalse(validator.validate(properties).isEmpty());
    }

    @Test
    void deveRejeitarRemetenteNulo() {
        EmailProperties properties = valida();
        properties.setRemetente(null);

        assertFalse(validator.validate(properties).isEmpty());
    }
}