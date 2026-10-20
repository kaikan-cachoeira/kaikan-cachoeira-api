package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.config.AsyncConfig;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

@SpringJUnitConfig({AsyncConfig.class, EmailService.class})
class EmailServiceAsyncTest {

    private static final EmailMensagem MENSAGEM =
            new EmailMensagem("a@teste.com", "Assunto", "<p>oi</p>");

    @Autowired private EmailService emailService;
    @MockitoBean private EmailEnviador enviador;

    @Test
    void deveExecutarOEnvioEmThreadDoExecutorDeEmail() throws Exception {
        CompletableFuture<String> thread = new CompletableFuture<>();
        doAnswer(inv -> {
            thread.complete(Thread.currentThread().getName());
            return null;
        }).when(enviador).enviar(MENSAGEM);

        emailService.enviar(MENSAGEM);

        String nome = thread.get(5, TimeUnit.SECONDS);
        assertThat(nome).startsWith("email-").isNotEqualTo(Thread.currentThread().getName());
    }

    @Test
    void falhaNoEnvioNaoDeveVazarParaQuemChamou() throws Exception {
        CompletableFuture<Void> chamado = new CompletableFuture<>();
        doAnswer(inv -> {
            chamado.complete(null);
            throw new EmailException("falha", null, false);
        }).when(enviador).enviar(MENSAGEM);

        assertThatCode(() -> emailService.enviar(MENSAGEM)).doesNotThrowAnyException();
        chamado.get(5, TimeUnit.SECONDS);
    }
}