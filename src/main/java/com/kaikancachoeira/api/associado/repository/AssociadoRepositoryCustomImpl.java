package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.entity.Associado;
import com.kaikancachoeira.api.associado.entity.Anuidade;
import com.kaikancachoeira.api.associado.entity.ConfiguracaoAnuidade;
import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
public class AssociadoRepositoryCustomImpl implements AssociadoRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public Page<AssociadoListaRow> buscar(
            String q,
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoAssociado tipo,
            Integer ano,
            StatusAnuidade status,
            Pageable pageable
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();

        CriteriaQuery<AssociadoListaRow> query = cb.createQuery(AssociadoListaRow.class);
        Root<Associado> associado = query.from(Associado.class);
        Join<Associado, Associado> titular = associado.join("titular", JoinType.LEFT);
        Join<Associado, Anuidade> anuidade = associado.join("anuidades", JoinType.LEFT);
        Join<Anuidade, ConfiguracaoAnuidade> configuracao = anuidade.join("configuracaoAnuidade", JoinType.LEFT);
        anuidade.on(cb.equal(configuracao.get("ano"), ano));

        Expression<String> statusDaAnuidade = cb.<String>coalesce()
                .value(anuidade.get("status"))
                .value(StatusAnuidade.PENDENTE.name());

        List<Predicate> predicados = criarPredicados(cb, associado, titular, q, dataInicio, dataFim, tipo, statusDaAnuidade, status);

        query.select(cb.construct(
                        AssociadoListaRow.class,
                        associado.get("id"),
                        associado.get("nome"),
                        associado.get("numeroContato"),
                        associado.get("dataNascimento"),
                        associado.get("tipo"),
                        titular.get("id"),
                        statusDaAnuidade
                ))
                .where(predicados.toArray(Predicate[]::new))
                .distinct(true)
                .orderBy(cb.asc(associado.get("nome")), cb.asc(associado.get("id")));

        List<AssociadoListaRow> itens = entityManager.createQuery(query)
                .setFirstResult(Math.toIntExact(pageable.getOffset()))
                .setMaxResults(pageable.getPageSize())
                .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Associado> associadoCount = countQuery.from(Associado.class);
        Join<Associado, Anuidade> anuidadeCount = associadoCount.join("anuidades", JoinType.LEFT);
        Join<Anuidade, ConfiguracaoAnuidade> configuracaoCount = anuidadeCount.join("configuracaoAnuidade", JoinType.LEFT);
        anuidadeCount.on(cb.equal(configuracaoCount.get("ano"), ano));

        Expression<String> statusCount = cb.<String>coalesce()
                .value(anuidadeCount.get("status"))
                .value(StatusAnuidade.PENDENTE.name());

        countQuery.select(cb.countDistinct(associadoCount.get("id")))
                .where(criarPredicados(cb, associadoCount,
                        associadoCount.join("titular", JoinType.LEFT), q, dataInicio, dataFim, tipo, statusCount, status)
                        .toArray(Predicate[]::new));

        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(itens, pageable, total);
    }

    private List<Predicate> criarPredicados(
            CriteriaBuilder cb,
            Root<Associado> associado,
            Join<Associado, Associado> titular,
            String q,
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoAssociado tipo,
            Expression<String> statusDaAnuidade,
            StatusAnuidade status
    ) {
        List<Predicate> predicados = new ArrayList<>();

        if (q != null && !q.isBlank()) {
            String termo = "%" + q.trim().toLowerCase(java.util.Locale.ROOT) + "%";
            predicados.add(cb.or(
                    cb.like(cb.lower(associado.get("nome")), termo),
                    cb.like(cb.lower(cb.coalesce(titular.get("nome"), "")), termo)
            ));
        }

        if (dataInicio != null) {
            predicados.add(cb.greaterThanOrEqualTo(associado.get("dataNascimento"), dataInicio));
        }

        if (dataFim != null) {
            predicados.add(cb.lessThanOrEqualTo(associado.get("dataNascimento"), dataFim));
        }

        if (tipo != null) {
            predicados.add(cb.equal(associado.get("tipo"), tipo));
        }

        if (status != null) {
            predicados.add(cb.equal(statusDaAnuidade, status.name()));
        }

        return predicados;
    }
}
