package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.venda.entity.ItemVenda;

import java.math.BigDecimal;

@Schema(description = "Detalhes de um item registrado em uma venda, preservando os valores praticados no momento da transação.")
public record ItemVendaDetalheResponse(
        @Schema(description = "Identificador do produto no cardápio do evento.")
        Long produtoEventoId,
        @Schema(description = "Nome do produto no momento da venda.")
        String nome,
        @Schema(description = "Quantidade vendida.")
        Integer quantidade,
        @Schema(description = "Preço unitário registrado no momento da venda, antes do desconto unitário.")
        BigDecimal precoUnitario,
        @Schema(description = "Desconto por unidade registrado no momento da venda.")
        BigDecimal descontoUnitario) {
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