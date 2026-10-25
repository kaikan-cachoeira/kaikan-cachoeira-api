package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Estoque;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record EstoqueResponse(
        Long id,
        @Schema(description = "Quantidade na entrada do lote") Integer quantidadeInicial,
        @Schema(description = "Saldo atual do lote. Lote zerado permanece como histórico") Integer quantidadeAtual,
        @Schema(description = "Custo por unidade. 0 em lotes sem custo (doação, ajuste)") BigDecimal custoUnitario,
        @Schema(description = "Valor pago pelo lote inteiro") BigDecimal custoTotal,
        @Schema(description = "Data de entrada do lote") Instant criadoEm
) {

    public static EstoqueResponse de(Estoque e) {
        return new EstoqueResponse(e.getId(), e.getQuantidadeInicial(), e.getQuantidadeAtual(),
                e.getCustoUnitario(), e.getCustoTotal(), e.getCriadoEm());
    }
}
