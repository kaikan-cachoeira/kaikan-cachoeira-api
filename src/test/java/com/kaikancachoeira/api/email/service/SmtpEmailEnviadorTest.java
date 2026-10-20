package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.config.EmailProperties;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.MailParseException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmtpEmailEnviadorTest {

    private static final EmailMensagem MENSAGEM =
            new EmailMensagem("dest@teste.com", "Recuperação de senha", "<p>Olá, ação</p>");

    @Mock private JavaMailSender javaMailSender;

    private SmtpEmailEnviador enviador;

    @BeforeEach
    void setUp() {
        EmailProperties properties = new EmailProperties();
        properties.setRemetente("no-reply@kaikan.test");
        when(javaMailSender.createMimeMessage())
                .thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        enviador = new SmtpEmailEnviador(javaMailSender, properties);
    }

    @Test
    void deveMontarEEnviarAMensagemHtml() throws Exception {
        enviador.enviar(MENSAGEM);

        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(javaMailSender).send(captor.capture());
        MimeMessage mime = captor.getValue();
        mime.saveChanges();

        assertThat(mime.getFrom()[0].toString()).isEqualTo("no-reply@kaikan.test");
        assertThat(mime.getAllRecipients()).hasSize(1);
        assertThat(mime.getAllRecipients()[0].toString()).isEqualTo("dest@teste.com");
        assertThat(mime.getSubject()).isEqualTo("Recuperação de senha");
        assertThat(mime.getContentType()).startsWith("text/html");
        assertThat(mime.getContent()).isEqualTo("<p>Olá, ação</p>");
    }

    @Test
    void falhaDeAutenticacaoDeveSerDefinitiva() {
        doThrow(new MailAuthenticationException("credenciais"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> enviador.enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isFalse());
    }

    @Test
    void falhaDeParseDeveSerDefinitiva() {
        doThrow(new MailParseException("parse"))
                .when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> enviador.enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isFalse());
    }

    @Test
    void falhaGenericaDeEnvioDeveSerTransitoria() {
        MailSendException causa = new MailSendException("smtp fora do ar");
        doThrow(causa).when(javaMailSender).send(any(MimeMessage.class));

        assertThatThrownBy(() -> enviador.enviar(MENSAGEM))
                .isInstanceOfSatisfying(EmailException.class, e -> {
                    assertThat(e.isTransitorio()).isTrue();
                    assertThat(e.getCause()).isSameAs(causa);
                });
    }

    @Test
    void destinatarioInvalidoDeveSerDefinitivoESemTentarEnviar() {
        EmailMensagem invalida = new EmailMensagem("a@@b", "Assunto", "<p>x</p>");

        assertThatThrownBy(() -> enviador.enviar(invalida))
                .isInstanceOfSatisfying(EmailException.class,
                        e -> assertThat(e.isTransitorio()).isFalse());

        verify(javaMailSender, never()).send(any(MimeMessage.class));
    }
}