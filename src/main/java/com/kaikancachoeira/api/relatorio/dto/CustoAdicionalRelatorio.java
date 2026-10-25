package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record CustoAdicionalRelatorio(
        String nome,
        @Schema(nullable = true) String descricao,
        BigDecimal valor
) {}