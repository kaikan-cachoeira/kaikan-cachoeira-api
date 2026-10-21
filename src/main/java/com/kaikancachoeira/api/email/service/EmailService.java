package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.config.AsyncConfig;
import com.kaikancachoeira.api.email.dto.EmailMensagem;
import com.kaikancachoeira.api.email.exception.EmailException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final EmailEnviador enviador;

    public EmailService(EmailEnviador enviador) {
        this.enviador = enviador;
    }

    @Async(value = AsyncConfig.EMAIL_EXECUTOR)
    public void enviar(EmailMensagem mensagem) {
        try {
            enviador.enviar(mensagem);
        } catch (EmailException ex) {
            if (ex.isTransitorio()) {
                log.warn("Falha transitória ao enviar email: {}", ex.getMessage(), ex);
            } else {
                log.error("Falha definitiva ao enviar email: {}", ex.getMessage(), ex);
            }
        } catch (RuntimeException e) {
            log.error("Erro inesperado ao enviar email", e);
        }
    }

}
