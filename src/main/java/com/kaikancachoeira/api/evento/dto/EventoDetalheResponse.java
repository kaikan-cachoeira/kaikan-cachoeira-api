package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;

import java.time.Instant;

public record EventoDetalheResponse(
        Long id,
        String nome,
        String observacoes,
        TipoEvento tipoEvento,
        StatusResumo status,
        Instant dataHoraInicio,
        Instant dataHoraEncerramento,
        Instant iniciadoEm,
        Instant encerradoEm,
        ResumoEvento resumo
) {

    public static EventoDetalheResponse de(Evento e, ResumoEvento resumo) {
        return new EventoDetalheResponse(
                e.getId(),
                e.getNome(),
                e.getObservacoes(),
                e.getTipoEvento(),
                StatusResumo.de(e.getStatus()),
                e.getInicioPrevistoEm(),
                e.getEncerramentoPrevistoEm(),
                e.getIniciadoEm(),
                e.getEncerradoEm(),
                resumo);
    }

}
