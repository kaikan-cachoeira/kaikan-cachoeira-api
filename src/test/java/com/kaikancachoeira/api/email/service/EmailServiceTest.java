package com.kaikancachoeira.api.email.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    private static final EmailMensagem MENSAGEM =
            new EmailMensagem("a@teste.com", "Assunto", "<p>oi</p>");

    @Mock private EmailEnviador enviador;

    private EmailService service;
    private ListAppender<ILoggingEvent> logs;
    private Logger logger;

    @BeforeEach
    void setUp() {
        service = new EmailService(enviador);
        logger = (Logger) LoggerFactory.getLogger(EmailService.class);
        logs = new ListAppender<>();
        logs.start();
        logger.addAppender(logs);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(logs);
    }

    @Test
    void deveDelegarOEnvioAoEnviador() {
        service.enviar(MENSAGEM);

        verify(enviador).enviar(MENSAGEM);
        assertThat(logs.list).isEmpty();
    }

    @Test
    void falhaTransitoriaNaoDevePropagarELogaWarn() {
        doThrow(new EmailException("timeout", null, true)).when(enviador).enviar(MENSAGEM);

        assertThatCode(() -> service.enviar(MENSAGEM)).doesNotThrowAnyException();

        assertThat(logs.list).singleElement().satisfies(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.WARN);
            assertThat(e.getFormattedMessage()).contains("transitória");
        });
    }

    @Test
    void falhaDefinitivaNaoDevePropagarELogaError() {
        doThrow(new EmailException("config", null, false)).when(enviador).enviar(MENSAGEM);

        assertThatCode(() -> service.enviar(MENSAGEM)).doesNotThrowAnyException();

        assertThat(logs.list).singleElement().satisfies(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.ERROR);
            assertThat(e.getFormattedMessage()).contains("definitiva");
        });
    }

    @Test
    void erroInesperadoNaoDevePropagarELogaError() {
        doThrow(new IllegalStateException("bug")).when(enviador).enviar(MENSAGEM);

        assertThatCode(() -> service.enviar(MENSAGEM)).doesNotThrowAnyException();

        assertThat(logs.list).singleElement().satisfies(e -> {
            assertThat(e.getLevel()).isEqualTo(Level.ERROR);
            assertThat(e.getFormattedMessage()).contains("inesperado");
        });
    }
}