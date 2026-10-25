package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta da exclusão de um lançamento de caixa.")
public record ExclusaoCaixaResponse(
        @Schema(description = "Resumo financeiro do caixa após a exclusão.")
        ResumoCaixaResponse resumo
) {}