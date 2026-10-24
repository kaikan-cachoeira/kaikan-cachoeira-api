package com.kaikancachoeira.api.evento.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventoRequest(
        @NotBlank @Size(max = 150) String nome,
        String observacoes,
        @NotNull TipoEvento tipoEvento,
        @NotNull Instant dataHoraInicio,
        @NotNull Instant dataHoraEncerramento
) {

    @JsonIgnore
    @AssertTrue(message = "A data de encerramento deve ser posterior à data de início")
    public boolean isPeriodoValido() {
        if (dataHoraInicio == null || dataHoraEncerramento == null) {
            return true;
        }

        return dataHoraEncerramento.isAfter(dataHoraInicio);
    }

}
