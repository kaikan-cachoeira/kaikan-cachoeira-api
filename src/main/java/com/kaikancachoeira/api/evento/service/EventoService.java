package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.evento.dto.EventoDetalheResponse;
import com.kaikancachoeira.api.evento.dto.EventoRequest;
import com.kaikancachoeira.api.evento.dto.EventoResponse;
import com.kaikancachoeira.api.evento.dto.ResumoEvento;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.StatusEvento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.repository.EventoSpecifications;
import com.kaikancachoeira.api.evento.repository.StatusEventoRepository;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Service
public class EventoService {

    private static final String NAO_INICIADO = "NAO_INICIADO";
    private static final String EM_ANDAMENTO = "EM_ANDAMENTO";

    private final EventoRepository eventoRepository;
    private final StatusEventoRepository statusEventoRepository;

    public EventoService(EventoRepository eventoRepository, StatusEventoRepository statusEventoRepository) {
        this.eventoRepository = eventoRepository;
        this.statusEventoRepository = statusEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<EventoResponse> listar(
            String q,
            Integer statusId,
            TipoEvento tipoEvento,
            Instant dataInicio,
            Instant dataFim
    ) {
        Specification<Evento> spec = Specification.unrestricted();

        if (q != null && !q.isBlank()) {
            spec = spec.and(EventoSpecifications.nomeContem(q.trim()));
        }

        if (statusId != null) {
            spec = spec.and(EventoSpecifications.comStatus(statusId));
        }

        if (tipoEvento != null) {
            spec = spec.and(EventoSpecifications.comTipo(tipoEvento));
        }

        if (dataInicio != null) {
            spec = spec.and(EventoSpecifications.inicioA_partirDe(dataInicio));
        }

        if (dataFim != null) {
            spec = spec.and(EventoSpecifications.inicioAte(dataFim));
        }

        return eventoRepository
                .findAll(spec, Sort.by(Sort.Direction.DESC, "inicioPrevistoEm"))
                .stream()
                .sorted(Comparator.comparing(e -> !EM_ANDAMENTO.equals(e.getStatus().getCodigo())))
                .map(EventoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventoDetalheResponse buscar(Long id) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", id));

        ResumoEvento resumo = new ResumoEvento(
                eventoRepository.contarProdutos(id),
                eventoRepository.contarVendasAtivas(id),
                eventoRepository.somarVendasAtivas(id),
                eventoRepository.somarCustosAdicionais(id)
        );

        return EventoDetalheResponse.de(evento, resumo);
    }

    @Transactional
    public EventoResponse criar(EventoRequest request) {
        String nome = request.nome().trim();

        if (eventoRepository.existsByNomeAndInicioPrevistoEmAndEncerramentoPrevistoEm(
                nome, request.dataHoraInicio(), request.dataHoraEncerramento()
        )) {
            throw eventoDuplicado();
        }

        StatusEvento status = statusEventoRepository.findByCodigo(NAO_INICIADO)
                .orElseThrow(() -> new IllegalStateException("Status NAO_INICIADO não encontrado"));

        Evento evento = new Evento();
        evento.setStatus(status);
        preencher(evento, nome, request);

        return EventoResponse.de(eventoRepository.saveAndFlush(evento));
    }

    @Transactional
    public EventoResponse atualizar(Long id, EventoRequest request) {
        Evento evento = eventoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", id));

        String nome = request.nome().trim();

        if (eventoRepository.existsByNomeAndInicioPrevistoEmAndEncerramentoPrevistoEmAndIdNot(
                nome, request.dataHoraInicio(), request.dataHoraEncerramento(), id
        )) {
            throw eventoDuplicado();
        }

        preencher(evento, nome, request);

        return EventoResponse.de(eventoRepository.saveAndFlush(evento));
    }

    private void preencher(Evento evento, String nome, EventoRequest request) {
        evento.setNome(nome);
        evento.setObservacoes(request.observacoes());
        evento.setTipoEvento(request.tipoEvento());
        evento.setInicioPrevistoEm(request.dataHoraInicio());
        evento.setEncerramentoPrevistoEm(request.dataHoraEncerramento());
    }

    private ConflitoException eventoDuplicado() {
        return new ConflitoException("EVENTO_DUPLICADO", "Já existe um evento com este nome e período.");
    }
}
