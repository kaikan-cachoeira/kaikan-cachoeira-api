package com.kaikancachoeira.api.relatorio.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimentacaoRelatorio(
        Long id,
        String tipo,
        Long produtoId,
        String produtoNome,
        int quantidade,
        BigDecimal custoUnitario,
        BigDecimal custoTotal,
        String observacoes,
        Instant criadoEm
) {
}
