package com.kaikancachoeira.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ValidacaoException extends ApiException {

    private final List<CampoErro> campos;

    public ValidacaoException(String mensagem, List<CampoErro> campos) {
        super(HttpStatus.BAD_REQUEST, GlobalExceptionHandler.VALIDACAO, mensagem);
        this.campos = campos;
    }

    public static ValidacaoException campo(String campo, String mensagem) {
        return new ValidacaoException("Dados inválidos", List.of(new CampoErro(campo, mensagem)));
    }
}
