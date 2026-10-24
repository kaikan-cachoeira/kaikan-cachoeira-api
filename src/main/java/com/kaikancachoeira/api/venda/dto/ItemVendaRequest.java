package com.kaikancachoeira.api.venda.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record ItemVendaRequest(
        @NotNull(message = "Produto é obrigatório")
        Long produtoEventoId,

        @NotNull(message = "Quantidade é obrigatória")
        @Positive(message = "Quantidade deve ser maior que 0")
        Integer quantidade
) {}
