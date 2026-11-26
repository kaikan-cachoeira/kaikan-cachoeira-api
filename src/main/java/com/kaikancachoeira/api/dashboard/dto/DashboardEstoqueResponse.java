package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resumo atual do estoque para o dashboard")
public record DashboardEstoqueResponse(
        @Schema(description = "Produtos abaixo do estoque mínimo") List<DashboardProdutoAlertaResponse> produtosAbaixoDoMinimo,
        @Schema(description = "Produtos com quantidade negativa") List<DashboardProdutoAlertaResponse> produtosNegativos
) {}
