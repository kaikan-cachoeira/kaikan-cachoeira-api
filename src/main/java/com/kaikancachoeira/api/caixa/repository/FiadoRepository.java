package com.kaikancachoeira.api.caixa.repository;

import com.kaikancachoeira.api.caixa.entity.Fiado;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FiadoRepository extends JpaRepository<Fiado, Long> {

    @Query("""
            select f from Fiado f join f.lancamentoOrigem p
            where p.evento.id = :eventoId
              and (:quitado is null or f.quitado = :quitado)
            order by f.criadoEm desc, f.id desc
            """)
    List<Fiado> listar(@Param("eventoId") Long eventoId, @Param("quitado") Boolean quitado);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Fiado f where f.id = :id")
    Optional<Fiado> buscarParaAtualizacao(@Param("id") Long id);

    @Query("select f from Fiado f join fetch f.lancamentoOrigem p where p.id in :lancamentoIds")
    List<Fiado> buscarPorLancamentosOrigem(@Param("lancamentoIds") Collection<Long> lancamentoIds);

    @Query("select f from Fiado f where f.lancamentoOrigem.id = :lancamentoId")
    Optional<Fiado> buscarPorLancamentoOrigem(@Param("lancamentoId") Long lancamentoId);

    @Query("""
            select sum(f.valor) from Fiado f join f.lancamentoOrigem p
            where p.evento.id = :eventoId and f.quitado = false
            """)
    BigDecimal somarEmAberto(@Param("eventoId") Long eventoId);

    @Query("""
            select count(f) from Fiado f join f.lancamentoOrigem p
            where p.evento.id = :eventoId and f.quitado = false
            """)
    long contarEmAberto(@Param("eventoId") Long eventoId);
}
