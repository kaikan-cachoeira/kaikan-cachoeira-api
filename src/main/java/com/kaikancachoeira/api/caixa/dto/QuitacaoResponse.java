package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resultado da quitação de um fiado, incluindo o fiado atualizado, o lançamento gerado e o resumo do caixa.")
public record QuitacaoResponse(
        @Schema(description = "Dados atualizados do fiado quitado.")
        FiadoResponse fiado,
        @Schema(description = "Lançamento de caixa criado para registrar a quitação.")
        LancamentoResponse lancamento,
        @Schema(description = "Resumo atualizado do caixa.")
        ResumoCaixaResponse resumo
) {}