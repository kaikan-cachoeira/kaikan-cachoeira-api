package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Schema(description = "Detalhes de uma venda, incluindo itens, valores e situação de cancelamento.")
public record VendaDetalheResponse(
        @Schema(description = "Identificador da venda.")
        Long id,
        @Schema(description = "Data e hora em que a venda foi registrada em ISO-8601.")
        Instant criadoEm,
        @Schema(description = "Itens que compõem a venda, com preços registrados no momento da venda.")
        List<ItemVendaDetalheResponse> itens,
        @Schema(description = "Desconto aplicado ao valor total da venda.")
        BigDecimal desconto,
        @Schema(description = "Valor total cobrado pela venda.")
        BigDecimal precoTotal,
        @Schema(description = "Indica se a venda foi cancelada.")
        boolean cancelada,
        @Schema(description = "Data e hora do cancelamento em ISO-8601; nulo se a venda não foi cancelada.")
        Instant canceladaEm) {
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