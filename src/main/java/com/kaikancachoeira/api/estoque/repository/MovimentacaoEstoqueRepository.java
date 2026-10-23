package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface MovimentacaoEstoqueRepository extends JpaRepository<MovimentacaoEstoque, Long> {

    @Query(value = """
            select m from MovimentacaoEstoque m join fetch m.tipo join fetch m.estoque e
            where e.produto.id = :produtoId
              and (:tipoId is null or m.tipo.id = :tipoId)
              and (:eventoId is null or m.eventoId = :eventoId)
              and m.criadoEm >= :dataInicio
              and m.criadoEm <= :dataFim
            order by m.criadoEm desc, m.id desc
            """,
            countQuery = """
            select count(m) from MovimentacaoEstoque m
            where m.estoque.produto.id = :produtoId
              and (:tipoId is null or m.tipo.id = :tipoId)
              and (:eventoId is null or m.eventoId = :eventoId)
              and m.criadoEm >= :dataInicio
              and m.criadoEm <= :dataFim
            """)
    Page<MovimentacaoEstoque> buscarPorProduto(@Param("produtoId") Long produtoId,
                                               @Param("tipoId") Integer tipoId,
                                               @Param("eventoId") Long eventoId,
                                               @Param("dataInicio") Instant dataInicio,
                                               @Param("dataFim") Instant dataFim,
                                               Pageable pageable);

    @Query(value = """
            select m from MovimentacaoEstoque m join fetch m.tipo join fetch m.estoque e
            where e.id = :estoqueId
            order by m.criadoEm desc, m.id desc
            """,
            countQuery = "select count(m) from MovimentacaoEstoque m where m.estoque.id = :estoqueId")
    Page<MovimentacaoEstoque> buscarPorLote(@Param("estoqueId") Long estoqueId, Pageable pageable);

    @Query(value = "select tipo_evento from evento where id = :eventoId", nativeQuery = true)
    Optional<String> findTipoEvento(@Param("eventoId") Long eventoId);
}