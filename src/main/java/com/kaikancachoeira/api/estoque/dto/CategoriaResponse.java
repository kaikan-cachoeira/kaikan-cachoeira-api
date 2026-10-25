package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Categoria;
import io.swagger.v3.oas.annotations.media.Schema;

public record CategoriaResponse(
        @Schema(example = "1") Integer id,
        @Schema(example = "BEBIDA") String nome,
        @Schema(nullable = true) String descricao
) {
    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}