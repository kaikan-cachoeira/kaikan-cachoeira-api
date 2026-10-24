package com.kaikancachoeira.api.venda.repository;

import com.kaikancachoeira.api.venda.entity.Venda;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendaRepository extends JpaRepository<Venda, Long> {

    Optional<Venda> findByIdAndEventoId(Long id, Long eventoId);
}
