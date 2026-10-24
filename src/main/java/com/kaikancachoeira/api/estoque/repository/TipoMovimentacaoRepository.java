package com.kaikancachoeira.api.estoque.repository;

import com.kaikancachoeira.api.estoque.entity.TipoMovimentacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TipoMovimentacaoRepository extends JpaRepository<TipoMovimentacao, Integer> {

    List<TipoMovimentacao> findByAtivoTrueAndGeradoPeloSistemaFalseOrderByIdAsc();

    Optional<TipoMovimentacao> findByCodigo(String codigo);
}