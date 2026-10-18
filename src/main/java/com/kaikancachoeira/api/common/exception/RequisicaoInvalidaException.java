package com.kaikancachoeira.api.common.exception;

import org.springframework.http.HttpStatus;

public class RequisicaoInvalidaException extends ApiException {
    public RequisicaoInvalidaException(String codigo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }
}