package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resumo de uma venda exibida no histórico de vendas do evento.")
public record VendaHistoricoResponse(
        @Schema(description = "Identificador da venda.")
        Long id,
        @Schema(description = "Data e hora em que a venda foi registrada em ISO-8601.")
        Instant criadoEm,
        @Schema(description = "Desconto aplicado ao valor total da venda.")
        BigDecimal desconto,
        @Schema(description = "Valor total cobrado pela venda.")
        BigDecimal precoTotal,
        @Schema(description = "Soma das quantidades de todos os itens da venda.")
        Integer quantidadeItens,
        @Schema(description = "Indica se a venda foi cancelada.")
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