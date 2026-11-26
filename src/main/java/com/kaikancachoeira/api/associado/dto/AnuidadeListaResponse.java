package com.kaikancachoeira.api.associado.dto;

import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Resumo da anuidade exibido na listagem de associados titulares.")
public record AnuidadeListaResponse(
        @Schema(example = "55")
        Long id,
        @Schema(example = "180.00")
        BigDecimal valor,
        @Schema(example = "0.00")
        BigDecimal totalPago,
        @Schema(example = "PENDENTE")
        StatusAnuidade status
) {}
