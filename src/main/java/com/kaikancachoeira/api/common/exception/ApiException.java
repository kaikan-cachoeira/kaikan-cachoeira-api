package com.kaikancachoeira.api.common.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String codigo;
    private final List<CampoErro> campos;

    public ApiException(HttpStatus status, String codigo, String mensagem) {
        this(status, codigo, mensagem, List.of());
    }

    public ApiException(HttpStatus status, String codigo, String mensagem, List<CampoErro> campos) {
        super(mensagem);
        this.status = status;
        this.codigo = codigo;
        this.campos = campos == null ? List.of() : List.copyOf(campos);
    }
}
