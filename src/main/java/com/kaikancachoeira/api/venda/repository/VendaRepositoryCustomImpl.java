package com.kaikancachoeira.api.venda.repository;

import com.kaikancachoeira.api.venda.entity.ItemVenda;
import com.kaikancachoeira.api.venda.entity.Venda;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Repository
public class VendaRepositoryCustomImpl implements VendaRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<Venda> buscarHistorico(
            Long eventoId,
            String q,
            Instant inicio,
            Instant fim,
            Boolean cancelada,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<Venda> query = cb.createQuery(Venda.class);
        Root<Venda> venda = query.from(Venda.class);

        query.select(venda)
                .where(criarPredicados(
                        cb, query, venda,
                        eventoId, q, inicio, fim, cancelada
                ).toArray(Predicate[]::new))
                .orderBy(
                        cb.desc(venda.get("criadoEm")),
                        cb.desc(venda.get("id"))
                );

        List<Venda> resultados = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Venda> vendaCount = countQuery.from(Venda.class);

        countQuery.select(cb.count(vendaCount))
                .where(criarPredicados(
                        cb, countQuery, vendaCount,
                        eventoId, q, inicio, fim, cancelada
                ).toArray(Predicate[]::new));

        Long total = entityManager.createQuery(countQuery)
                .getSingleResult();

        return new PageImpl<>(resultados, pageable, total);
    }

    private List<Predicate> criarPredicados(
            CriteriaBuilder cb,
            CriteriaQuery<?> query,
            Root<Venda> venda,
            Long eventoId,
            String q,
            Instant inicio,
            Instant fim,
            Boolean cancelada
    ) {
        List<Predicate> predicados = new ArrayList<>();

        predicados.add(
                cb.equal(venda.get("evento").get("id"), eventoId)
        );

        if (inicio != null) {
            predicados.add(
                    cb.greaterThanOrEqualTo(
                            venda.get("criadoEm"),
                            inicio
                    )
            );
        }

        if (fim != null) {
            predicados.add(
                    cb.lessThanOrEqualTo(
                            venda.get("criadoEm"),
                            fim
                    )
            );
        }

        if (cancelada != null) {
            if (cancelada) {
                predicados.add(
                        cb.isNotNull(venda.get("canceladaEm"))
                );
            } else {
                predicados.add(
                        cb.isNull(venda.get("canceladaEm"))
                );
            }
        }

        if (q != null && !q.isBlank()) {
            String termo = "%"
                    + q.trim().toLowerCase(Locale.ROOT)
                    + "%";

            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<ItemVenda> item = subquery.from(ItemVenda.class);

            subquery.select(cb.literal(1))
                    .where(
                            cb.equal(item.get("venda"), venda),
                            cb.like(
                                    cb.lower(
                                            item.get("produtoEvento")
                                                    .<String>get("nome")
                                    ),
                                    termo
                            )
                    );

            predicados.add(cb.exists(subquery));
        }

        return predicados;
    }
}