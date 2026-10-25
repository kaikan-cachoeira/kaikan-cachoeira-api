package com.kaikancachoeira.api.common.exception;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Erro de validação em um campo")
public record CampoErro(
        @Schema(description = "Nome do campo", example = "valor") String campo,
        @Schema(description = "Mensagem do erro", example = "Valor deve ser maior que 0") String mensagem
) {}
