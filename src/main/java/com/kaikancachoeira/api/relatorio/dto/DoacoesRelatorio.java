package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

public record DoacoesRelatorio(
        @Schema(description = "Doações de produto e de reabastecimento") List<DoacaoItemRelatorio> itens,
        @Schema(description = "Doações em dinheiro") List<DoacaoDinheiroRelatorio> emDinheiro,
        BigDecimal totalEmDinheiro
) {}