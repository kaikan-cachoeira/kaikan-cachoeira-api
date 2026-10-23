package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Estoque;

import java.math.BigDecimal;
import java.time.Instant;

public record EstoqueResponse(
        Long id,
        Integer quantidadeInicial,
        Integer quantidadeAtual,
        BigDecimal custoUnitario,
        BigDecimal custoTotal,
        Instant criadoEm
) {

    public static EstoqueResponse de(Estoque e) {
        return new EstoqueResponse(e.getId(), e.getQuantidadeInicial(), e.getQuantidadeAtual(),
                e.getCustoUnitario(), e.getCustoTotal(), e.getCriadoEm());
    }
}
