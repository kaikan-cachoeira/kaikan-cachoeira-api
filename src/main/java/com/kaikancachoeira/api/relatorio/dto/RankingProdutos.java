package com.kaikancachoeira.api.relatorio.dto;

import java.util.List;

public record RankingProdutos(
        long quantidadeVendasTotais,
        List<ProdutoRanking> ranking
) {
}
