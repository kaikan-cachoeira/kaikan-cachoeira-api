package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Produto com alerta de estoque")
public record DashboardProdutoAlertaResponse(
        @Schema(description = "ID do produto") Long produtoId,
        @Schema(description = "Nome do produto") String nome,
        @Schema(description = "Quantidade atual em estoque") Integer quantidadeAtual,
        @Schema(description = "Estoque mínimo configurado") Integer estoqueMinimo
) {}
