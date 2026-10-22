package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.Estoque;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EstoqueRepository extends JpaRepository<Estoque, Long> {

    List<Estoque> findByProdutoIdOrderByCriadoEmAscIdAsc(Long produtoId);

    List<Estoque> findByProdutoIdAndQuantidadeAtualGreaterThanOrderByCriadoEmAscIdAsc(Long produtoId, Integer quantidade);

    boolean existsByProdutoId(Long produtoId);
}