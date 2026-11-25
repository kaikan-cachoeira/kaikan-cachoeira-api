package com.kaikancachoeira.api.associado.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

@Schema(description = "Dados para registrar um pagamento de anuidade.")
public record PagamentoRequest(
        @Schema(example = "10")
        @NotNull
        Integer confAnuidadeId,

        @Schema(example = "180.00")
        @NotNull @Positive
        BigDecimal valor
) {}
