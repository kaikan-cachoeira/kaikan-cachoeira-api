package com.kaikancachoeira.api.email.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EmailExceptionTest {

    @Test
    void devePreservarMensagemCausaEFlagTransitorio() {
        RuntimeException causa = new RuntimeException("origem");

        EmailException ex = new EmailException("falha", causa, true);

        assertThat(ex.getMessage()).isEqualTo("falha");
        assertThat(ex.getCause()).isSameAs(causa);
        assertThat(ex.isTransitorio()).isTrue();
    }

    @Test
    void deveIndicarFalhaDefinitiva() {
        assertThat(new EmailException("falha", null, false).isTransitorio()).isFalse();
    }
}