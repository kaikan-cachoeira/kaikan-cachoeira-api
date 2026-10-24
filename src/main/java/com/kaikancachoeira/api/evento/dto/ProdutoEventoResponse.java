package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;

import java.math.BigDecimal;
import java.time.Instant;

public record ProdutoEventoResponse(
        Long id,
        OrigemProdutoEvento origem,
        Long produtoId,
        String nome,
        String descricao,
        String categoria,
        String fotoUrl,
        BigDecimal precoVenda,
        BigDecimal desconto,
        Instant descontoAplicadoEm,
        BigDecimal precoComDesconto,
        Integer quantidadePrevista,
        Integer quantidadeVendida,
        Integer restantePrevisto,
        BigDecimal custoTotalIngredientes,
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