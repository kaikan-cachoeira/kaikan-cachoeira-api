package com.kaikancachoeira.api.estoque.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProdutoRequest(
        @NotBlank @Size(max = 150) String nome,
        String descricao,
        Integer categoriaId,
        @PositiveOrZero Integer estoqueMinimo
) {}