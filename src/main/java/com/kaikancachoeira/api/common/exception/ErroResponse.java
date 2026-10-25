package com.kaikancachoeira.api.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Corpo padrão de erro")
public record ErroResponse(
        @Schema(description = "Código estável do erro", example = "SALDO_INSUFICIENTE") String codigo,
        @Schema(description = "Mensagem legível", example = "Saldo insuficiente. Disponível: 2.") String mensagem,
        @Schema(description = "Erros por campo (vazio quando não é validação)") List<CampoErro> campos
) {
    public static ErroResponse de(String codigo, String mensagem) {
        return new ErroResponse(codigo, mensagem, List.of());
    }
}