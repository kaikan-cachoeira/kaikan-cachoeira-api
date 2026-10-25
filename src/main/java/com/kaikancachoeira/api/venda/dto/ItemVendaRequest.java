package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemVendaRequest(
        @Schema(description = "ID do item no cardápio do evento", example = "7")
        @NotNull(message = "Produto é obrigatório")
        Long produtoEventoId,

        @Schema(description = "Quantidade vendida", example = "3")
        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que 0")
        Integer quantidade
) {}
