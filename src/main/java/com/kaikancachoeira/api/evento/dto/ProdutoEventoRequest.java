package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProdutoEventoRequest(
        @Schema(description = "ESTOQUE (produto cadastrado) ou PRODUZIDO (feito no evento, sem estoque)")
        @NotNull
        OrigemProdutoEvento origem,

        @Schema(description = "Obrigatório em ESTOQUE; não usado em PRODUZIDO", example = "3")
        Long produtoId,

        @Schema(description = "Obrigatório em PRODUZIDO, único no evento. Em ESTOQUE vem do produto", example = "Tempurá")
        @Size(max = 150)
        String nome,

        @Schema(description = "Só em PRODUZIDO. Em ESTOQUE vem do produto")
        String descricao,

        @Schema(description = "Só em PRODUZIDO, opcional. Deve ser o nome de uma categoria existente", example = "COMIDA")
        @Size(max = 100)
        String categoria,

        @Schema(description = "Preço de venda unitário", example = "10.00")
        @NotNull
        @PositiveOrZero
        BigDecimal precoVenda,

        @Schema(description = "Obrigatório em ESTOQUE; não usado em PRODUZIDO", example = "20")
        @Positive
        Integer quantidadePrevista,

        @Schema(description = "Obrigatório em PRODUZIDO: custo total dos ingredientes no evento", example = "40.00")
        @PositiveOrZero
        BigDecimal custoTotalIngredientes
) {}