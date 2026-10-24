package com.kaikancachoeira.api.venda.dto;

import com.kaikancachoeira.api.venda.entity.Venda;

import java.math.BigDecimal;
import java.time.Instant;

public record VendaResponse(
        Long id,
        Instant criadoEm,
        BigDecimal desconto,
        BigDecimal precoTotal
) {
    public static VendaResponse de(Venda venda) {
        return new VendaResponse(venda.getId(), venda.getCriadoEm(), venda.getDesconto(), venda.getPrecoTotal());
    }
}
