package com.kaikancachoeira.api.associados.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para criação de anuidade.")
public record AnuidadeRequest(
        @Schema(example = "1")
        @NotNull
        Integer idConfAnuidade,

        @Schema(example = "180.00")
        @NotNull @Positive
        BigDecimal valor
) {}
