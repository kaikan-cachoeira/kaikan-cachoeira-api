package com.kaikancachoeira.api.relatorio.dto;

import java.math.BigDecimal;

public record CustoAdicionalRelatorio(
        String nome,
        String descricao,
        BigDecimal valor
) {
}
