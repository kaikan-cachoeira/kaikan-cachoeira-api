package com.kaikancachoeira.api.associados.dto;

import com.kaikancachoeira.api.associados.entity.Associado;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "Representação de um associado.")
public record AssociadoResponse(
        Long id,
        String nome,
        String numeroContato,
        LocalDate dataNascimento,
        TipoAssociado tipo,
        Long titularId
) {
    public static AssociadoResponse de(Associado associado) {
        return new AssociadoResponse(
                associado.getId(),
                associado.getNome(),
                associado.getNumeroContato(),
                associado.getDataNascimento(),
                associado.getTipo(),
                associado.getTitular() == null ? null : associado.getTitular().getId()
        );
    }
}
