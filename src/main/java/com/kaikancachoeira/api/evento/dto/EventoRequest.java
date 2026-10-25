package com.kaikancachoeira.api.evento.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record EventoRequest(
        @Schema(description = "Nome do evento. Espaços externos são removidos", example = "Undoukai 2026")
        @NotBlank @Size(max = 150)
        String nome,

        @Schema(description = "Observações (opcional)")
        String observacoes,

        @Schema(description = "EVENTO (com cardápio e vendas) ou REUNIAO (só despesa)", example = "EVENTO")
        @NotNull
        TipoEvento tipoEvento,

        @Schema(description = "Início previsto (ISO-8601)", example = "2026-11-01T09:00:00Z")
        @NotNull
        Instant dataHoraInicio,

        @Schema(description = "Encerramento previsto (ISO-8601). Deve ser posterior ao início; "
                + "se não for, o erro vem no campo periodoValido", example = "2026-11-01T17:00:00Z")
        @NotNull
        Instant dataHoraEncerramento
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
