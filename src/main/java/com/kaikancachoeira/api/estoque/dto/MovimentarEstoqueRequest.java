package com.kaikancachoeira.api.estoque.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MovimentarEstoqueRequest(
        @NotNull Integer tipoMovimentacaoId,
        @NotNull @Positive Integer quantidade,
        @Positive BigDecimal custoTotal,
        Long eventoId,
        String observacoes
) {}