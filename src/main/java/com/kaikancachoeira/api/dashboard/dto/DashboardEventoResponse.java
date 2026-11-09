package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Série financeira por evento para o dashboard")
public record DashboardEventoResponse(
        @Schema(description = "ID do evento") Long eventoId,
        @Schema(description = "Nome do evento") String nome,
        @Schema(description = "Data de início real do evento") Instant data,
        @Schema(description = "Receita do evento") BigDecimal receita,
        @Schema(description = "Custo do evento") BigDecimal custo,
        @Schema(description = "Lucro do evento") BigDecimal lucro
) {}
