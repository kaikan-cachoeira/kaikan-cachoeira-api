package com.kaikancachoeira.api.auth.exception;

import com.kaikancachoeira.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class CredenciaisInvalidasException extends ApiException {
    public CredenciaisInvalidasException() {
        super(HttpStatus.UNAUTHORIZED, "CREDENCIAIS_INVALIDAS", "Usuário ou senha inválidos");
    }
}
