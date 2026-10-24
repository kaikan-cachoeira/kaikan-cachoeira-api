package com.kaikancachoeira.api.evento.repository;

import com.kaikancachoeira.api.evento.entity.StatusEvento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusEventoRepository extends JpaRepository<StatusEvento, Integer> {

    Optional<StatusEvento> findByCodigo(String codigo);

}
