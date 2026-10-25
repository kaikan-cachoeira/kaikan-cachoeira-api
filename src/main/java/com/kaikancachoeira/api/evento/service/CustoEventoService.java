package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.evento.dto.CustoEventoRequest;
import com.kaikancachoeira.api.evento.dto.CustoEventoResponse;
import com.kaikancachoeira.api.evento.entity.CustoEvento;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.repository.CustoEventoRepository;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CustoEventoService {

    private final EventoRepository eventoRepository;
    private final CustoEventoRepository custoEventoRepository;

    public CustoEventoService(
            EventoRepository eventoRepository,
            CustoEventoRepository custoEventoRepository
    ) {
        this.eventoRepository = eventoRepository;
        this.custoEventoRepository = custoEventoRepository;
    }

    @Transactional(readOnly = true)
    public List<CustoEventoResponse> listar(Long eventoId) {
        eventoRepository.findById(eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de("Evento", eventoId)
                );

        return custoEventoRepository.findByEventoIdOrderById(eventoId)
                .stream()
                .map(CustoEventoResponse::de)
                .toList();
    }

    @Transactional
    public CustoEventoResponse adicionar(
            Long eventoId,
            CustoEventoRequest request
    ) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de("Evento", eventoId)
                );

        validarStatusPermitido(evento);

        CustoEvento custo = new CustoEvento();
        custo.setEvento(evento);
        custo.setNome(request.nome().trim());
        custo.setDescricao(request.descricao());
        custo.setValor(request.valor());

        CustoEvento salvo = custoEventoRepository.saveAndFlush(custo);

        return CustoEventoResponse.de(salvo);
    }

    @Transactional
    public CustoEventoResponse atualizar(
            Long eventoId,
            Long custoId,
            CustoEventoRequest request
    ) {
        Evento evento = eventoRepository.findById(eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de("Evento", eventoId)
                );

        CustoEvento custo = custoEventoRepository
                .findByIdAndEventoId(custoId, eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de(
                                "Custo do evento", custoId
                        )
                );

        validarStatusPermitido(evento);

        custo.setNome(request.nome().trim());
        custo.setDescricao(request.descricao());
        custo.setValor(request.valor());

        CustoEvento atualizado = custoEventoRepository.saveAndFlush(custo);

        return CustoEventoResponse.de(atualizado);
    }

    @Transactional
    public void remover(Long eventoId, Long custoId) {
        eventoRepository.findById(eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de("Evento", eventoId)
                );

        CustoEvento custo = custoEventoRepository
                .findByIdAndEventoId(custoId, eventoId)
                .orElseThrow(() ->
                        RecursoNaoEncontradoException.de(
                                "Custo do evento", custoId
                        )
                );

        custoEventoRepository.delete(custo);
        custoEventoRepository.flush();
    }

    private void validarStatusPermitido(Evento evento) {
        String status = evento.getStatus().getCodigo();

        if (!"EM_ANDAMENTO".equals(status)
                && !"ENCERRADO".equals(status)) {
            throw new ConflitoException(
                    "CUSTO_EVENTO_STATUS_INVALIDO",
                    "Só é possível adicionar ou atualizar custos "
                            + "de eventos em andamento ou encerrados."
            );
        }
    }
}