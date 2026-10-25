package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimentacaoRelatorio(
        Long id,
        @Schema(description = "Código do tipo", example = "PERDA") String tipo,
        Long produtoId,
        String produtoNome,
        @Schema(description = "Valor absoluto da quantidade movimentada") int quantidade,
        BigDecimal custoUnitario,
        BigDecimal custoTotal,
        @Schema(nullable = true) String observacoes,
        Instant criadoEm
) {}
