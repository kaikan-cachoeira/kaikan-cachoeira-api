package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProdutoEventoRepository extends JpaRepository<ProdutoEvento, Long> {

    Optional<ProdutoEvento> findByIdAndEventoId(Long id, Long eventoId);

    List<ProdutoEvento> findByEventoIdAndIdIn(Long eventoId, Collection<Long> ids);
}
