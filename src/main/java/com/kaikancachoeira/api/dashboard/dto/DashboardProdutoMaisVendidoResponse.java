package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Produto com maior receita no período")
public record DashboardProdutoMaisVendidoResponse(
        @Schema(description = "ID do produto") Long produtoId,
        @Schema(description = "Nome do produto") String nome,
        @Schema(description = "Quantidade vendida") long quantidade,
        @Schema(description = "Receita total do produto") BigDecimal receita,
        @Schema(description = "Participação percentual sobre a receita total da lista") BigDecimal participacao,
        @Schema(description = "Participação percentual acumulada") BigDecimal participacaoAcumulada
) {}
