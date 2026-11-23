package com.kaikancachoeira.api.associado.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para cadastro/atualização de configuração de anuidade.")
public record ConfiguracaoAnuidadeRequest(
        @Schema(example = "2026")
        @NotNull
        Integer ano,

        @Schema(example = "180.00")
        @NotNull @Positive
        BigDecimal valor
) {}
