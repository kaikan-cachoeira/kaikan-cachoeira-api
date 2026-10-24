package com.kaikancachoeira.api.caixa.repository;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PagamentoCaixaRepository extends JpaRepository<PagamentoCaixa, Long> {

    Optional<PagamentoCaixa> findByIdAndEventoId(Long id, Long eventoId);

    @Query(value = """
            select p from PagamentoCaixa p left join fetch p.fiadoQuitado
            where p.evento.id = :eventoId
              and (:tipo is null or p.tipo = :tipo)
              and (:forma is null or p.formaPagamento = :forma)
            order by p.criadoEm desc, p.id desc
            """,
            countQuery = """
            select count(p) from PagamentoCaixa p
            where p.evento.id = :eventoId
              and (:tipo is null or p.tipo = :tipo)
              and (:forma is null or p.formaPagamento = :forma)
            """)
    Page<PagamentoCaixa> buscar(@Param("eventoId") Long eventoId,
                                @Param("tipo") TipoLancamento tipo,
                                @Param("forma") FormaPagamento forma,
                                Pageable pageable);

    @Query("""
            select new com.kaikancachoeira.api.caixa.repository.TotalPorTipoForma(p.tipo, p.formaPagamento, sum(p.valor))
            from PagamentoCaixa p
            where p.evento.id = :eventoId
            group by p.tipo, p.formaPagamento
            """)
    List<TotalPorTipoForma> totaisPorTipoEForma(@Param("eventoId") Long eventoId);
}
