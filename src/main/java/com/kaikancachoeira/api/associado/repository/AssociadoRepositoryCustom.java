package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.dto.AssociadoListaResponse;
import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;
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
