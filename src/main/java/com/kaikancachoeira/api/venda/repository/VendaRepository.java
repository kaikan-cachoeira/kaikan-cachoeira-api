package com.kaikancachoeira.api.venda.repository;

import com.kaikancachoeira.api.venda.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long>, VendaRepositoryCustom {

    Optional<Venda> findByIdAndEventoId(Long id, Long eventoId);

    Page<Venda> buscarHistorico(
            @Param("eventoId") Long eventoId,
            @Param("q") String q,
            @Param("inicio") Instant inicio,
            @Param("fim") Instant fim,
            @Param("cancelada") Boolean cancelada,
            Pageable pageable
    );

}
