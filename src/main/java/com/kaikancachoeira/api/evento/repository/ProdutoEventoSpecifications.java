package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class ProdutoEventoSpecifications {

    private ProdutoEventoSpecifications() {}

    public static Specification<ProdutoEvento> doEvento(Long eventoId) {
        return (root, query, cb) -> cb.equal(root.get("evento").get("id"), eventoId);
    }

    public static Specification<ProdutoEvento> textoContem(String q) {
        return (root, query, cb) -> cb.like(cb.lower(root.get("nome")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<ProdutoEvento> categoriasEm(Collection<String> categorias) {
        return (root, query, cb) -> root.get("categoria").in(categorias);
    }

    public static Specification<ProdutoEvento> comOrigem(OrigemProdutoEvento origem) {
        return (root, query, cb) -> cb.equal(root.get("origem"), origem);
    }

    public static Specification<ProdutoEvento> ordenarPorId() {
        return (root, query, cb) -> {
            query.orderBy(cb.asc(root.get("id")));
            return cb.conjunction();
        };
    }
}
