package com.kaikancachoeira.api.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustoEventoRequest(

        @Schema(
                description = "Nome do custo",
                example = "Limpeza",
                minLength = 1,
                maxLength = 150
        )
        @NotBlank
        @Size(min = 1, max = 150)
        String nome,

        @Schema(
                description = "Descrição (opcional)",
                example = "Faxineira contratada",
                maxLength = 255
        )
        @Size(max = 255)
        String descricao,

        @Schema(
                description = "Valor, maior que zero",
                example = "120.00",
                minimum = "0",
                exclusiveMinimum = true
        )
        @NotNull
        @DecimalMin(value = "0", inclusive = false, message = "O valor deve ser maior que zero")
        BigDecimal valor
) {}