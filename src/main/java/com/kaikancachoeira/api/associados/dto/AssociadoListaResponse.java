package com.kaikancachoeira.api.associados.dto;

import com.kaikancachoeira.api.associados.entity.Associado;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Linha da listagem de associados com status da anuidade do ano consultado.")
public record AssociadoListaResponse(
        Long id,
        String nome,
        String numeroContato,
        LocalDate dataNascimento,
        TipoAssociado tipo,
        Long titularId,
        StatusAnuidade status
) {
    public static AssociadoListaResponse de(Associado associado, StatusAnuidade status) {
        return new AssociadoListaResponse(
                associado.getId(),
                associado.getNome(),
                associado.getNumeroContato(),
                associado.getDataNascimento(),
                associado.getTipo(),
                associado.getTitular() == null ? null : associado.getTitular().getId(),
                status
        );
    }
}
