package com.kaikancachoeira.api.venda.repository;

import com.kaikancachoeira.api.venda.entity.Venda;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;

public interface VendaRepositoryCustom {

    Page<Venda> buscarHistorico(
            Long eventoId,
            String q,
            Instant inicio,
            Instant fim,
            Boolean cancelada,
            Pageable pageable
    );
}