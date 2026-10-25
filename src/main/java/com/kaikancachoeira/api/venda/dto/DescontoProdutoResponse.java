package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.evento.entity.ProdutoEvento;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Dados do preço de venda do produto no evento após a aplicação ou remoção de desconto.")
public record DescontoProdutoResponse(
        @Schema(description = "Identificador do produto no cardápio do evento.")
        Long id,
        @Schema(description = "Preço unitário original do produto no evento.")
        BigDecimal precoVenda,
        @Schema(description = "Valor do desconto aplicado por unidade.")
        BigDecimal desconto,
        @Schema(description = "Data e hora em que o desconto foi aplicado; nulo quando não há desconto aplicado.")
        Instant descontoAplicadoEm,
        @Schema(description = "Preço unitário após a aplicação do desconto.")
        BigDecimal precoComDesconto
) {
    public static DescontoProdutoResponse de(ProdutoEvento produtoEvento) {
        return new DescontoProdutoResponse(produtoEvento.getId(), produtoEvento.getPrecoVenda(),
                produtoEvento.getDesconto(), produtoEvento.getDescontoAplicadoEm(),
                produtoEvento.getPrecoComDesconto());
    }
}