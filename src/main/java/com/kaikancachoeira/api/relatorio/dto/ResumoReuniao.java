package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ResumoReuniao(
        BigDecimal custoCompras,
        BigDecimal custoConsumoInterno,
        BigDecimal totalCustosAdicionais,
        @Schema(description = "custoCompras + custoConsumoInterno + totalCustosAdicionais") BigDecimal custoTotal
) {}