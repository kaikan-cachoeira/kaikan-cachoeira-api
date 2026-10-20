package com.kaikancachoeira.api.auth.exception;

import com.kaikancachoeira.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TokenSenhaInvalidoOuExpiradoException extends ApiException {
    public TokenSenhaInvalidoOuExpiradoException() {
        super(HttpStatus.BAD_REQUEST, "TOKEN_INVALIDO", "Token inválido ou expirado");
    }
}
