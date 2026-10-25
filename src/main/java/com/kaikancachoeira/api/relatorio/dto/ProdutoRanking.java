package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record ProdutoRanking(
        Long produtoEventoId,
        String nome,
        @Schema(nullable = true) String categoria,
        long quantidadeVendida
) {}

