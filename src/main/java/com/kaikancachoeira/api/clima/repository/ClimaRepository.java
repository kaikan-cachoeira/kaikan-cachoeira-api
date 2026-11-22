package com.kaikancachoeira.api.clima.repository;

import com.kaikancachoeira.api.clima.entity.Clima;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface ClimaRepository extends JpaRepository<Clima, Long> {

    List<Clima> findByEventoIdOrderByDataHoraPrevistaAsc(Long eventoId);

    boolean existsByEventoIdAndDataHoraPrevista(Long eventoId, Instant dataHoraPrevista);
}
