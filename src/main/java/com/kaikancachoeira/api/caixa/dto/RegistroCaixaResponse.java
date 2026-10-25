package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado do registro de um lançamento de caixa.")
public record RegistroCaixaResponse(
        @Schema(description = "Lançamento de caixa registrado.")
        LancamentoResponse lancamento,
        @Schema(description = "Resumo financeiro atualizado do caixa.")
        ResumoCaixaResponse resumo
) {}