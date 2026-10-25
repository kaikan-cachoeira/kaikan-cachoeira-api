package com.kaikancachoeira.api.venda.dto;

import com.kaikancachoeira.api.venda.entity.ItemVenda;

import java.math.BigDecimal;

public record ItemVendaDetalheResponse(
        Long produtoEventoId,
        String nome,
        Integer quantidade,
        BigDecimal precoUnitario,
        BigDecimal descontoUnitario
) {
    public static ItemVendaDetalheResponse de(ItemVenda item) {
        return new ItemVendaDetalheResponse(
                item.getProdutoEvento().getId(),
                item.getProdutoEvento().getNome(),
                item.getQuantidade(),
                item.getPrecoUnitario(),
                item.getDescontoUnitario()
        );
    }
}