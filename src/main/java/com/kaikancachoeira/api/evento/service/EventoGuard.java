package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import org.springframework.stereotype.Component;

@Component
public class EventoGuard {

    public static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    public static final String ENCERRADO = "ENCERRADO";

    private final EventoRepository eventoRepository;

    public EventoGuard(EventoRepository eventoRepository) {
        this.eventoRepository = eventoRepository;
    }

    public Evento buscar(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", eventoId));
    }

    public Evento buscarComLock(Long eventoId) {
        return eventoRepository.findByIdParaAtualizacao(eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", eventoId));
    }

    public Evento exigirEmAndamento(Long eventoId) {
        Evento evento = buscarComLock(eventoId);
        if (!EM_ANDAMENTO.equals(evento.getStatus().getCodigo())) {
            throw new ConflitoException("EVENTO_NAO_EM_ANDAMENTO", "O evento não está em andamento.");
        }
        return evento;
    }
}
