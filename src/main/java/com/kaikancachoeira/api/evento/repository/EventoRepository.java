package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.Evento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;

public interface EventoRepository extends JpaRepository<Evento, Long>, JpaSpecificationExecutor<Evento> {

    boolean existsByNomeAndInicioPrevistoEmAndEncerramentoPrevistoEm(String nome, Instant inicioPrevistoEm, Instant encerramentoPrevistoEm);

    boolean existsByNomeAndInicioPrevistoEmAndEncerramentoPrevistoEmAndIdNot(String nome, Instant inicioPrevistoEm, Instant encerramentoPrevistoEm, Long id);

    @Query(value = """
                SELECT COUNT(*)
                FROM produto_evento
                WHERE evento_id = :id
            """, nativeQuery = true)
    long contarProdutos(@Param("id") Long id);

    @Query(value = """
            SELECT COUNT(*)
            FROM venda v
            WHERE v.evento_id = :id 
                AND v.cancelada_em IS NULL
            """, nativeQuery = true)
    long contarVendasAtivas(@Param("id") Long id);

    @Query(value = """
            SELECT COALESCE(SUM(v.preco_total), 0)
            FROM venda v
            WHERE v.evento_id = :id
                AND v.cancelada_em IS NULL
            """, nativeQuery = true)
    BigDecimal somarVendasAtivas(@Param("id") Long id);

    @Query(value = """
            SELECT COALESCE(SUM(c.valor), 0)
            FROM custo_evento c
            WHERE c.evento_id = :id
            """, nativeQuery = true)
    BigDecimal somarCustosAdicionais(@Param("id") Long id);

}
