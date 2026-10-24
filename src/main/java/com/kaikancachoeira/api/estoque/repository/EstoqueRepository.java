package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.Estoque;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    List<Estoque> findByProdutoIdOrderByCriadoEmAscIdAsc(Long produtoId);

    List<Estoque> findByProdutoIdAndQuantidadeAtualGreaterThanOrderByCriadoEmAscIdAsc(Long produtoId, Integer quantidade);

    boolean existsByProdutoId(Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e from Estoque e
            where e.produto.id = :produtoId and e.quantidadeAtual > 0
            order by e.criadoEm asc, e.id asc
            """)
    List<Estoque> lotesDisponiveisParaBaixa(@Param("produtoId") Long produtoId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Estoque e where e.id in :ids order by e.id")
    List<Estoque> travarPorIds(@Param("ids") Collection<Long> ids);
}