package com.kaikancachoeira.api.associado.repository;

import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssociadoListaRow(
        Long id,
        String nome,
        String numeroContato,
        LocalDate dataNascimento,
        TipoAssociado tipo,
        Long titularId,
        String titularNome,
        StatusAnuidade status,
        Long anuidadeId,
        BigDecimal anuidadeValor,
        BigDecimal anuidadeTotalPago,
        StatusAnuidade anuidadeStatus
) {
}
