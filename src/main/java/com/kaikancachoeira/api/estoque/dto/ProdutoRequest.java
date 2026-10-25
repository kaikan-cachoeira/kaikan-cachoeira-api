package com.kaikancachoeira.api.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @Schema(description = "Nome do produto. Único, sem diferenciar maiúsculas", example = "Coca-Cola 350ml")
        @NotBlank @Size(max = 150)
        String nome,

        @Schema(description = "Descrição opcional", example = "Lata 350ml")
        String descricao,

        @Schema(description = "ID da categoria (opcional)", example = "1")
        Integer categoriaId,

        @Schema(description = "Quantidade mínima em estoque (opcional). Abaixo ou igual a ela o produto aparece como estoque baixo", example = "10")
        @PositiveOrZero
        Integer estoqueMinimo
) {}