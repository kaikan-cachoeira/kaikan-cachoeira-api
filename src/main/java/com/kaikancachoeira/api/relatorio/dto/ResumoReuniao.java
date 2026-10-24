package com.kaikancachoeira.api.relatorio.dto;

import java.math.BigDecimal;

public record ResumoReuniao(
        BigDecimal custoCompras,
        BigDecimal custoConsumoInterno,
        BigDecimal totalCustosAdicionais,
        BigDecimal custoTotal
) {
}
