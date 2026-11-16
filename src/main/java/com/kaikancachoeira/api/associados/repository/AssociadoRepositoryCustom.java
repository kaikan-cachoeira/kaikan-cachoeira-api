package com.kaikancachoeira.api.associados.repository;

import com.kaikancachoeira.api.associados.dto.AssociadoListaResponse;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;

public interface AssociadoRepositoryCustom {

    Page<AssociadoListaResponse> buscar(
            String q,
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoAssociado tipo,
            Integer ano,
            StatusAnuidade status,
            Pageable pageable
    );
}
