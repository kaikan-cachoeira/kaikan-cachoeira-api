package com.kaikancachoeira.api.common.exception;

import org.springframework.http.HttpStatus;

import java.util.List;

public class RequisicaoInvalidaException extends ApiException {
    public RequisicaoInvalidaException(String codigo, String mensagem) {
        super(HttpStatus.BAD_REQUEST, codigo, mensagem);
    }

    public RequisicaoInvalidaException(String codigo, String mensagem, List<CampoErro> campos) {
        super(HttpStatus.BAD_REQUEST, codigo, mensagem, campos);
    }
}
