package com.kaikancachoeira.api.associados.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para atualização parcial do valor de uma anuidade.")
public record AnuidadeValorRequest(
        @Schema(example = "220.00")
        @NotNull @Positive
        BigDecimal valor
) {}
