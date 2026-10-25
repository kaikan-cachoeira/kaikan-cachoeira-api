package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record RankingProdutos(
        @Schema(description = "Total de unidades vendidas") long quantidadeVendasTotais,
        @Schema(description = "Produtos com venda, do mais vendido para o menos vendido") List<ProdutoRanking> ranking
) {}