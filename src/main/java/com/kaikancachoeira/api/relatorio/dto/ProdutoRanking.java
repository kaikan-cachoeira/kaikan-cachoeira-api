package com.kaikancachoeira.api.relatorio.dto;

public record ProdutoRanking(
        Long produtoEventoId,
        String nome,
        String categoria,
        long quantidadeVendida
) {
}
