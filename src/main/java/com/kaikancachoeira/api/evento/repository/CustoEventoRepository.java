package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.CustoEvento;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CustoEventoRepository extends JpaRepository<CustoEvento, Long> {

    void deleteByEventoId(Long eventoId);

}