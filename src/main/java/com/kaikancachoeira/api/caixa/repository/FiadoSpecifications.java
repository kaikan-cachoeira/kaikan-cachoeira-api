package com.kaikancachoeira.api.caixa.repository;

import com.kaikancachoeira.api.caixa.entity.Fiado;
import org.springframework.data.jpa.domain.Specification;

public final class FiadoSpecifications {

    private FiadoSpecifications() {}

    public static Specification<Fiado> doEvento(Long eventoId) {
        return (root, query, cb) -> cb.equal(root.get("lancamentoOrigem").get("evento").get("id"), eventoId);
    }

    public static Specification<Fiado> quitado(Boolean quitado) {
        return (root, query, cb) -> quitado == null ? cb.conjunction() : cb.equal(root.get("quitado"), quitado);
    }
}
