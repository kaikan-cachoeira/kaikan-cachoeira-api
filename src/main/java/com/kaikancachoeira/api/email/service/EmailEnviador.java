package com.kaikancachoeira.api.email.service;

import com.kaikancachoeira.api.email.dto.EmailMensagem;

public interface EmailEnviador {
    void enviar(EmailMensagem mensagem);
}
