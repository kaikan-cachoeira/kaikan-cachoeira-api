package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record ProdutoEventoResponse(
        Long id,
        OrigemProdutoEvento origem,
        @Schema(description = "Nulo em itens PRODUZIDO", nullable = true) Long produtoId,
        @Schema(description = "Snapshot do nome") String nome,
        @Schema(description = "Snapshot da descrição", nullable = true) String descricao,
        @Schema(description = "Snapshot do nome da categoria", example = "COMIDA", nullable = true) String categoria,
        @Schema(nullable = true) String fotoUrl,
        @Schema(description = "Preço de venda, sem desconto") BigDecimal precoVenda,
        @Schema(description = "Desconto por unidade. 0 quando não há desconto") BigDecimal desconto,
        @Schema(nullable = true) Instant descontoAplicadoEm,
        @Schema(description = "precoVenda - desconto") BigDecimal precoComDesconto,
        @Schema(description = "Nulo em itens PRODUZIDO", nullable = true) Integer quantidadePrevista,
        @Schema(description = "Soma das vendas não canceladas") Integer quantidadeVendida,
        @Schema(description = "quantidadePrevista - quantidadeVendida, sem piso em 0 (pode ficar negativo). Nulo em PRODUZIDO", nullable = true)
        Integer restantePrevisto,
        @Schema(description = "Só em itens PRODUZIDO", nullable = true) BigDecimal custoTotalIngredientes,
        Instant criadoEm,
        Instant atualizadoEm
) {
    public static ProdutoEventoResponse de(ProdutoEvento pe, Integer quantidadeVendida) {
        BigDecimal preco = pe.getPrecoVenda();
        BigDecimal desc = pe.getDesconto();
        BigDecimal precoComDesc = preco.subtract(desc);

        Long produtoId = pe.getProduto() != null ? pe.getProduto().getId() : null;
        Integer prevista = pe.getQuantidadePrevista();
        Integer restante = (prevista != null && quantidadeVendida != null)
                ? prevista - quantidadeVendida : null;

        return new ProdutoEventoResponse(
                pe.getId(),
                pe.getOrigem(),
                produtoId,
                pe.getNome(),
                pe.getDescricao(),
                pe.getCategoria(),
                ArmazenamentoService.url(pe.getFotoUrl()),
                preco,
                desc,
                pe.getDescontoAplicadoEm(),
                precoComDesc,
                prevista,
                quantidadeVendida,
                restante,
                pe.getCustoTotalIngredientes(),
                pe.getCriadoEm(),
                pe.getAtualizadoEm()
        );
    }
}