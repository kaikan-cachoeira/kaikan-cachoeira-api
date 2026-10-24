package com.kaikancachoeira.api.venda.dto;

import com.kaikancachoeira.api.evento.entity.ProdutoEvento;

import java.math.BigDecimal;
import java.time.Instant;

public record DescontoProdutoResponse(
        Long id,
        BigDecimal precoVenda,
        BigDecimal desconto,
        Instant descontoAplicadoEm,
        BigDecimal precoComDesconto
) {
    public static DescontoProdutoResponse de(ProdutoEvento produtoEvento) {
        return new DescontoProdutoResponse(produtoEvento.getId(), produtoEvento.getPrecoVenda(),
                produtoEvento.getDesconto(), produtoEvento.getDescontoAplicadoEm(),
                produtoEvento.getPrecoComDesconto());
    }
}
