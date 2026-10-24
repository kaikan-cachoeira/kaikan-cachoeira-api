package com.kaikancachoeira.api.evento.exception;

import com.kaikancachoeira.api.common.exception.ApiException;
import org.springframework.http.HttpStatus;

public class TransicaoStatusEventoInvalida extends ApiException {
    public TransicaoStatusEventoInvalida(String statusAtual, String novoStatus) {
        super(HttpStatus.CONFLICT, "TRANSICAO_EVENTO_INVALIDA",
                "Não é permitido transicionar o evento de %s para %s".formatted(statusAtual, novoStatus));
    }
}
