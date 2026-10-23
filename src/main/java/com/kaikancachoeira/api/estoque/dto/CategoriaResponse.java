package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Categoria;

public record CategoriaResponse(Integer id, String nome, String descricao) {

    public static CategoriaResponse de(Categoria categoria) {
        return new CategoriaResponse(categoria.getId(), categoria.getNome(), categoria.getDescricao());
    }
}