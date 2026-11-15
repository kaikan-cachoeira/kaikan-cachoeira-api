package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.estoque.enums.SituacaoProduto;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class ProdutoSpecifications {

    private ProdutoSpecifications() {}

    public static Specification<Produto> ativos() {
        return (root, query, cb) -> cb.isTrue(root.get("ativo"));
    }

    public static Specification<Produto> textoContem(String texto) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + texto.toLowerCase() + "%");
    }

    public static Specification<Produto> categoriasEm(Collection<Integer> categoriaIds) {
        return (root, query, cb) -> root.get("categoria").get("id").in(categoriaIds);
    }

    public static Specification<Produto> situacao(SituacaoProduto situacao) {
        return switch (situacao) {
            case OK -> ok();
            case OUT -> out();
            case LOW -> low();
        };
    }

    public static Specification<Produto> carregarCategoria() {
        return (root, query, cb) -> {
            if (query != null && Produto.class.equals(query.getResultType())) {
                root.fetch("categoria", JoinType.LEFT);
                query.distinct(true);
            }
            return cb.conjunction();
        };
    }

    private static Specification<Produto> ok() {
        return (root, query, cb) -> cb.and(
                cb.gt(root.get("quantidadeAtual"), 0),
                cb.or(
                        cb.isNull(root.get("estoqueMinimo")),
                        cb.gt(root.get("quantidadeAtual"), root.get("estoqueMinimo"))
                )
        );
    }

    private static Specification<Produto> out() {
        return (root, query, cb) -> cb.equal(root.get("quantidadeAtual"), 0);
    }

    private static Specification<Produto> low() {
        return (root, query, cb) -> cb.and(
                cb.isNotNull(root.get("estoqueMinimo")),
                cb.gt(root.get("quantidadeAtual"), 0),
                cb.lessThanOrEqualTo(root.get("quantidadeAtual"), root.get("estoqueMinimo"))
        );
    }
}
