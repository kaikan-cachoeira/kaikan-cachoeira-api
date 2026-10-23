package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.estoque.entity.Categoria;
import com.kaikancachoeira.api.estoque.entity.Produto;

public record ProdutoResponse(
        Long id,
        String nome,
        String descricao,
        CategoriaResumo categoria,
        String fotoUrl,
        Integer quantidadeAtual,
        Integer estoqueMinimo
) {

    public record CategoriaResumo(Integer id, String nome) {}

    public static ProdutoResponse de(Produto produto) {
        Categoria c = produto.getCategoria();
        return new ProdutoResponse(
                produto.getId(),
                produto.getNome(),
                produto.getDescricao(),
                c == null ? null : new CategoriaResumo(c.getId(), c.getNome()),
                ArmazenamentoService.url(produto.getFotoUrl()),
                produto.getQuantidadeAtual(),
                produto.getEstoqueMinimo());
    }
}