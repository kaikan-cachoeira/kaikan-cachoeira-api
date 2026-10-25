package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.estoque.entity.Categoria;
import com.kaikancachoeira.api.estoque.entity.Produto;
import io.swagger.v3.oas.annotations.media.Schema;

public record ProdutoResponse(
        @Schema(example = "1") Long id,
        @Schema(example = "Coca-Cola 350ml") String nome,
        String descricao,
        @Schema(nullable = true) CategoriaResumo categoria,
        @Schema(description = "Caminho relativo da foto, servida em /arquivos/{chave}", example = "/arquivos/3f2a....png", nullable = true)
        String fotoUrl,
        @Schema(description = "Soma dos saldos de todos os lotes", example = "24") Integer quantidadeAtual,
        @Schema(nullable = true, example = "10") Integer estoqueMinimo
) {

    @Schema(description = "Categoria resumida do produto")
    public record CategoriaResumo(@Schema(example = "1") Integer id, @Schema(example = "BEBIDA") String nome) {}

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