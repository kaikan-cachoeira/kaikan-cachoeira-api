package com.kaikancachoeira.api.venda.dto;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record VendaDetalheResponse(
        Long id,
        Instant criadoEm,
        List<ItemVendaDetalheResponse> itens,
        BigDecimal desconto,
        BigDecimal precoTotal,
        boolean cancelada,
        Instant canceladaEm
) {
    public static VendaDetalheResponse de(Venda venda) {
        List<ItemVendaDetalheResponse> itens = venda.getItens().stream()
                .map(ItemVendaDetalheResponse::de)
                .toList();

        return new VendaDetalheResponse(
                venda.getId(),
                venda.getCriadoEm(),
                itens,
                venda.getDesconto(),
                venda.getPrecoTotal(),
                venda.cancelada(),
                venda.getCanceladaEm()
        );
    }
}