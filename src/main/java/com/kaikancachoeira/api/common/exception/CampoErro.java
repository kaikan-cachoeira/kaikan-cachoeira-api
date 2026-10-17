package com.kaikancachoeira.api.common.exception;

public record CampoErro(
        String campo,
        String mensagem
) {}
