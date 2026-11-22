package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.Collection;

public final class EventoSpecifications {

    private static final String EM_ANDAMENTO = "EM_ANDAMENTO";

    private EventoSpecifications() {}

    public static Specification<Evento> nomeContem(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Evento> comStatus(Collection<String> codigos) {
        return (root, query, cb) ->
                root.get("status").get("codigo").in(codigos);
    }

    public static Specification<Evento> comTipos(Collection<TipoEvento> tipos) {
        return (root, query, cb) ->
                root.get("tipoEvento").in(tipos);
    }

    public static Specification<Evento> inicioA_partirDe(Instant data) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("inicioPrevistoEm"), data);
    }

    public static Specification<Evento> inicioAte(Instant data) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("inicioPrevistoEm"), data);
    }

    public static Specification<Evento> ordenarComEmAndamentoPrimeiro() {
        return (root, query, cb) -> {
            var prioridade = cb.selectCase()
                    .when(cb.equal(root.get("status").get("codigo"), EM_ANDAMENTO), 0)
                    .otherwise(1);

            query.orderBy(
                    cb.asc(prioridade),
                    cb.desc(root.get("inicioPrevistoEm")),
                    cb.desc(root.get("id"))
            );

            return cb.conjunction();
        };
    }
}
