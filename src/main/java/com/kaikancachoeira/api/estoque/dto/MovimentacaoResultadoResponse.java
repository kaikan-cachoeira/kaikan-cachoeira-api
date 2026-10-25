package com.kaikancachoeira.api.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record MovimentacaoResultadoResponse(
        @Schema(description = "Uma movimentação por lote criado (entrada) ou consumido (saída, FIFO)")
        List<MovimentacaoResponse> movimentacoes,
        @Schema(description = "Saldo do produto após a movimentação") Integer quantidadeAtual
) {}
