package com.kaikancachoeira.api.email.dto;

public record EmailMensagem(
        String destinatario, String assunto, String corpoHtml
) {}
