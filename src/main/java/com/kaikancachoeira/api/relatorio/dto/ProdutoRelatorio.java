package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;

import java.math.BigDecimal;

public record ProdutoRelatorio(
        Long produtoEventoId,
        String nome,
        OrigemProdutoEvento origem,
        Integer quantidadePrevista,
        long quantidadeVendida,
        BigDecimal totalDescontos,
        BigDecimal custoTotal,
        BigDecimal receitaVendas,
        BigDecimal lucroBruto,
        BigDecimal lucroLiquido,
        BigDecimal precoVendido
) {
}
