package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumo financeiro e operacional do dashboard")
public record DashboardResumoResponse(
        @Schema(description = "Quantidade total de eventos no período") long totalEventos,
        @Schema(description = "Receita total no período") BigDecimal receitaTotal,
        @Schema(description = "Custo total no período") BigDecimal custoTotal,
        @Schema(description = "Lucro total no período") BigDecimal lucroTotal
) {}
