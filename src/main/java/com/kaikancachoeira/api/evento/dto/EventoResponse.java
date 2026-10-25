package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

public record EventoResponse(
        Long id,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        @Schema(description = "Início previsto") Instant dataHoraInicio,
        @Schema(description = "Encerramento previsto") Instant dataHoraEncerramento,
        @Schema(description = "Quando o evento foi iniciado de fato", nullable = true) Instant iniciadoEm,
        @Schema(description = "Encerramento vigente. Limpo ao reabrir", nullable = true) Instant encerradoEm
) {

    public static EventoResponse de(Evento e) {
        return new EventoResponse(
                e.getId(),
                e.getNome(),
                e.getTipoEvento(),
                StatusResumo.de(e.getStatus()),
                e.getInicioPrevistoEm(),
                e.getEncerramentoPrevistoEm(),
                e.getIniciadoEm(),
                e.getEncerradoEm());
    }

}
