package com.kaikancachoeira.api.common.exception;

import java.util.List;

public record ErroResponse(
        String codigo,
        String mensagem,
        List<CampoErro> campos
) {
    public static ErroResponse de(String codigo, String mensagem) {
        return new ErroResponse(codigo, mensagem, List.of());
    }
}