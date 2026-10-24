package com.kaikancachoeira.api.doacao.repository;

import com.kaikancachoeira.api.doacao.entity.Doacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import org.springframework.data.jpa.domain.Specification;

import java.util.Collection;

public final class DoacaoSpecifications {

    private static final char ESCAPE = '\\';

    private DoacaoSpecifications() {}

    public static Specification<Doacao> textoContem(String q) {
        String padrao = "%" + escapar(q.toLowerCase()) + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("doador")), padrao, ESCAPE),
                cb.like(cb.lower(root.get("nome")), padrao, ESCAPE));
    }

    public static Specification<Doacao> comTipos(Collection<TipoDoacao> tipos) {
        return (root, query, cb) -> root.get("tipo").in(tipos);
    }

    public static Specification<Doacao> doEventos(Collection<Long> eventoIds) {
        return (root, query, cb) -> root.get("eventoId").in(eventoIds);
    }

    private static String escapar(String texto) {
        return texto
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
