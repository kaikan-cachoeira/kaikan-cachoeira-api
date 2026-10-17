package com.kaikancachoeira.api.common.exception;

import org.springframework.http.HttpStatus;

public class ConflitoException extends ApiException {
    public ConflitoException(String codigo, String mensagem) {
        super(HttpStatus.CONFLICT, codigo, mensagem);
    }
}
