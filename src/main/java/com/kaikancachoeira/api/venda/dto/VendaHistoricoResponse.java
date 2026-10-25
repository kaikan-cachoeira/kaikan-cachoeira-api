package com.kaikancachoeira.api.venda.dto;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;

public record VendaHistoricoResponse(
        Long id,
        Instant criadoEm,
        BigDecimal desconto,
        BigDecimal precoTotal,
        Integer quantidadeItens,
        boolean cancelada
) {
    public static VendaHistoricoResponse de(Venda venda) {
        int quantidadeItens = venda.getItens().stream()
                .mapToInt(item -> item.getQuantidade())
                .sum();

        return new VendaHistoricoResponse(
                venda.getId(),
                venda.getCriadoEm(),
                venda.getDesconto(),
                venda.getPrecoTotal(),
                quantidadeItens,
                venda.cancelada()
        );
    }
}