package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.CustoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CustoEventoRepository extends JpaRepository<CustoEvento, Long> {

    List<CustoEvento> findByEventoIdOrderById(Long eventoId);

    Optional<CustoEvento> findByIdAndEventoId(Long id, Long eventoId);

    void deleteByEventoId(Long eventoId);

}