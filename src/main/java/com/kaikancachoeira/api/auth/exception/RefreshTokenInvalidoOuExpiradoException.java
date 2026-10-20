package com.kaikancachoeira.api.auth.exception;

import com.kaikancachoeira.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class RefreshTokenInvalidoOuExpiradoException extends ApiException {
    public RefreshTokenInvalidoOuExpiradoException() {
        super(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_INVALIDO_OU_EXPIRADO", "Refresh token inválido ou expirado");
    }
}
