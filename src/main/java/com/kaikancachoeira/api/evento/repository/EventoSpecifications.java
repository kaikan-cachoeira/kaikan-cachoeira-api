package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class EventoSpecifications {

    private EventoSpecifications() {}

    public static Specification<Evento> nomeContem(String q) {
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("nome")), "%" + q.toLowerCase() + "%");
    }

    public static Specification<Evento> comStatus(Integer statusId) {
        return (root, query, cb) ->
                cb.equal(root.get("status").get("id"), statusId);
    }

    public static Specification<Evento> comTipo(TipoEvento tipo) {
        return (root, query, cb) ->
                cb.equal(root.get("tipoEvento"), tipo);
    }

    public static Specification<Evento> inicioA_partirDe(Instant data) {
        return (root, query, cb) ->
                cb.greaterThanOrEqualTo(root.get("inicioPrevistoEm"), data);
    }

    public static Specification<Evento> inicioAte(Instant data) {
        return (root, query, cb) ->
                cb.lessThanOrEqualTo(root.get("inicioPrevistoEm"), data);
    }
}
