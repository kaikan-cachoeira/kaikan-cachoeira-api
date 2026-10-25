package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Representação resumida da venda recém-registrada.")
public record VendaResponse(
        @Schema(description = "Identificador da venda.")
        Long id,
        @Schema(description = "Data e hora em que a venda foi registrada em ISO-8601.")
        Instant criadoEm,
        @Schema(description = "Desconto aplicado ao valor total da venda.")
        BigDecimal desconto,
        @Schema(description = "Valor total cobrado pela venda.")
        BigDecimal precoTotal) {
    public static VendaResponse de(Venda venda) {
        return new VendaResponse(venda.getId(), venda.getCriadoEm(), venda.getDesconto(), venda.getPrecoTotal());
    }
}