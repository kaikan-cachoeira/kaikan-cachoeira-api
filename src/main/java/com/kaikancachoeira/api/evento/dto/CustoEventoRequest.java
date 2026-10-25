package com.kaikancachoeira.api.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CustoEventoRequest(
        @Schema(description = "Nome do custo", example = "Limpeza")
        @NotBlank
        @Size(max = 150)
        String nome,

        @Schema(description = "Descrição (opcional)", example = "Faxineira contratada")
        @Size(max = 255)
        String descricao,

        @Schema(description = "Valor, maior que 0", example = "120.00")
        @NotNull
        @Positive
        BigDecimal valor
) {}