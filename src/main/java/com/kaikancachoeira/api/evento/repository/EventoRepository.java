package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

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

    boolean existsByTipoEventoAndStatusCodigoAndIdNot(TipoEvento tipoEvento, String codigo, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Evento e WHERE e.id = :id")
    Optional<Evento> findByIdParaAtualizacao(@Param("id") Long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM venda WHERE evento_id = :id)", nativeQuery = true)
    boolean possuiVendas(@Param("id") Long id);

    @Query(value = "SELECT EXISTS (SELECT 1 FROM pagamento_caixa WHERE evento_id = :id)", nativeQuery = true)
    boolean possuiLancamentosCaixa(@Param("id") Long id);
}
