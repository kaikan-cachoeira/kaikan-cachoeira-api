package com.kaikancachoeira.api.dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Associado com pendência de anuidade no ano corrente para o dashboard")
public record DashboardAssociadoPendenteResponse(
        @Schema(description = "ID do associado") Long associadoId,
        @Schema(description = "Nome do associado") String nome,
        @Schema(description = "Valor pendente da anuidade no ano") BigDecimal valorPendente
) {}
