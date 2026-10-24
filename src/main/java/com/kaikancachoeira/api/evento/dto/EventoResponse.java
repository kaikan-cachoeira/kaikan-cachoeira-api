package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;

import java.time.Instant;

public record EventoResponse(
        Long id,
        String nome,
        TipoEvento tipoEvento,
        StatusResumo status,
        Instant dataHoraInicio,
        Instant dataHoraEncerramento,
        Instant iniciadoEm,
        Instant encerradoEm
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
