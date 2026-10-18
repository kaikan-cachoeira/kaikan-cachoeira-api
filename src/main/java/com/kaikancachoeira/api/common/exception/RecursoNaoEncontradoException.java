package com.kaikancachoeira.api.common.exception;

import org.springframework.http.HttpStatus;

public class RecursoNaoEncontradoException extends ApiException {

    public static final String CODIGO_PADRAO = "RECURSO_NAO_ENCONTRADO";

    public RecursoNaoEncontradoException(String codigo, String mensagem) {
        super(HttpStatus.NOT_FOUND, codigo, mensagem);
    }

    public static RecursoNaoEncontradoException de(String recurso, Object id) {
        return new RecursoNaoEncontradoException(CODIGO_PADRAO, recurso + " não encontrado: " + id);
    }
}
