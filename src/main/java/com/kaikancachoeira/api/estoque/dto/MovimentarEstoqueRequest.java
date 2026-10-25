package com.kaikancachoeira.api.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record MovimentarEstoqueRequest(
        @Schema(description = "ID do tipo de movimentação manual (GET /tipos-movimentacao). Tipos gerados pelo sistema são rejeitados", example = "1")
        @NotNull
        Integer tipoMovimentacaoId,

        @Schema(description = "Quantidade sempre positiva; o sentido vem do tipo", example = "10")
        @NotNull @Positive
        Integer quantidade,

        @Schema(description = "Custo total pago. Obrigatório e maior que 0 quando o tipo exige custo (REABASTECIMENTO); ignorado nos demais", example = "45.00")
        @Positive
        BigDecimal custoTotal,

        @Schema(description = "Evento relacionado. Obrigatório em CONSUMO_INTERNO e deve ser do tipo REUNIAO", nullable = true)
        Long eventoId,

        @Schema(description = "Observações. Em PERDA, registra o motivo", example = "Latas amassadas")
        String observacoes
) {}
