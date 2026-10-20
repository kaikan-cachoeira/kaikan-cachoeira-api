package com.kaikancachoeira.api.email.config;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class ResendPropertiesTest {

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

    private ResendProperties valida() {
        ResendProperties properties = new ResendProperties();
        properties.setApiKey("re_chave");
        return properties;
    }

    @Test
    void deveAplicarPadroesDeBaseUrlETimeouts() {
        ResendProperties properties = valida();

        assertThat(validator.validate(properties)).isEmpty();
        assertThat(properties.getBaseUrl()).isEqualTo("https://api.resend.com");
        assertThat(properties.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
        assertThat(properties.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
    }

    @Test
    void deveRejeitarApiKeyEmBranco() {
        ResendProperties properties = valida();
        properties.setApiKey(" ");

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void deveRejeitarBaseUrlEmBranco() {
        ResendProperties properties = valida();
        properties.setBaseUrl("");

        assertThat(validator.validate(properties)).isNotEmpty();
    }

    @Test
    void deveRejeitarTimeoutsNulos() {
        ResendProperties semConnect = valida();
        semConnect.setConnectTimeout(null);
        ResendProperties semRead = valida();
        semRead.setReadTimeout(null);

        assertThat(validator.validate(semConnect)).isNotEmpty();
        assertThat(validator.validate(semRead)).isNotEmpty();
    }
}