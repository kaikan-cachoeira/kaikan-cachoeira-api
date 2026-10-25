package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.evento.dto.*;
import com.kaikancachoeira.api.evento.entity.CustoEvento;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.StatusEvento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.exception.TransicaoStatusEventoInvalida;
import com.kaikancachoeira.api.evento.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class EventoService {

    private static final String NAO_INICIADO = "NAO_INICIADO";
    private static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    private static final String ENCERRADO = "ENCERRADO";

    private static final Map<String, Set<String>> TRANSICOES = Map.of(
            NAO_INICIADO, Set.of(EM_ANDAMENTO),
            EM_ANDAMENTO, Set.of(ENCERRADO, NAO_INICIADO),
            ENCERRADO, Set.of(EM_ANDAMENTO));

    private final EventoRepository eventoRepository;
    private final StatusEventoRepository statusEventoRepository;
    private final CustoEventoRepository custoEventoRepository;
    private final Clock clock;
    private final ProdutoEventoRepository produtoEventoRepository;

    public EventoService(EventoRepository eventoRepository, StatusEventoRepository statusEventoRepository, CustoEventoRepository custoEventoRepository, Clock clock, ProdutoEventoRepository produtoEventoRepository) {
        this.eventoRepository = eventoRepository;
        this.statusEventoRepository = statusEventoRepository;
        this.custoEventoRepository = custoEventoRepository;
        this.clock = clock;
        this.produtoEventoRepository = produtoEventoRepository;
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

    @Transactional
    public void alterarStatus(Long id, AlteracaoStatusEventoRequest request) {
        Evento evento = eventoRepository.findByIdParaAtualizacao(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", id));

        StatusEvento novo = statusEventoRepository.findById(request.status())
                .orElseThrow(() -> new RequisicaoInvalidaException("STATUS_INVALIDO", "Status inválido"));

        String atual = evento.getStatus().getCodigo();
        String destino = novo.getCodigo();

        if (!TRANSICOES.getOrDefault(atual, Set.of()).contains(destino)) {
            throw new TransicaoStatusEventoInvalida(atual, destino);
        }

        boolean temCustos = request.custos() != null && !request.custos().isEmpty();
        if (temCustos && !ENCERRADO.equals(destino)) {
            throw new RequisicaoInvalidaException("CUSTOS_NAO_PERMITIDOS",
                    "Custos só podem ser informados ao encerrar o evento.");
        }

        switch (destino) {
            case EM_ANDAMENTO -> {
                validarSemOutroEmAndamento(evento);

                if (NAO_INICIADO.equals(atual)) {
                    evento.setIniciadoEm(clock.instant());
                } else {
                    evento.setEncerradoEm(null);
                }
            }
            case ENCERRADO -> {
                evento.setEncerradoEm(clock.instant());

                if (temCustos) {
                    salvarCustos(evento, request.custos());
                }
            }
            case NAO_INICIADO -> {
                if (eventoRepository.possuiVendas(id) || eventoRepository.possuiLancamentosCaixa(id)) {
                    throw new ConflitoException("DESFAZER_INICIO_BLOQUEADO",
                            "Não é possível desfazer o início: já existem vendas ou lançamentos de caixa.");
                }

                evento.setIniciadoEm(null);
            }
            default -> throw new IllegalStateException("Status não tratado: " + destino);
        }

        evento.setStatus(novo);
        eventoRepository.saveAndFlush(evento);
    }

    @Transactional
    public void excluir(Long id) {
        Evento evento = eventoRepository.findByIdParaAtualizacao(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", id));

        if (!NAO_INICIADO.equals(evento.getStatus().getCodigo())) {
            throw new ConflitoException(
                    "EXCLUSAO_EVENTO_STATUS_INVALIDO",
                    "Só é possível excluir eventos que ainda não foram iniciados."
            );
        }

        boolean possuiDependencias = eventoRepository.possuiVendas(id)
                || eventoRepository.possuiDoacoes(id)
                || eventoRepository.possuiMovimentacoesEstoque(id)
                || eventoRepository.possuiLancamentosCaixa(id);

        if (possuiDependencias) {
            throw new ConflitoException(
                    "EXCLUSAO_EVENTO_COM_REGISTROS",
                    "Não é possível excluir um evento que possui vendas, doações, movimentações de estoque ou lançamentos de caixa."
            );
        }

        produtoEventoRepository.deleteByEventoId(id);
        custoEventoRepository.deleteByEventoId(id);

        eventoRepository.delete(evento);
        eventoRepository.flush();
    }

    private void validarSemOutroEmAndamento(Evento evento) {
        boolean outroEmAndamento = evento.getTipoEvento() == TipoEvento.EVENTO
                && eventoRepository.existsByTipoEventoAndStatusCodigoAndIdNot(
                TipoEvento.EVENTO, EM_ANDAMENTO, evento.getId());

        if (outroEmAndamento) {
            throw new ConflitoException("EVENTO_EM_ANDAMENTO_EXISTENTE", "Já existe um evento em andamento.");
        }
    }

    private void salvarCustos(Evento evento, List<CustoEventoRequest> custos) {
        List<CustoEvento> entidades = custos.stream()
                .map(c -> {
                    CustoEvento custo = new CustoEvento();
                    custo.setEvento(evento);
                    custo.setNome(c.nome().trim());
                    custo.setDescricao(c.descricao());
                    custo.setValor(c.valor());
                    return custo;
                })
                .toList();

        custoEventoRepository.saveAll(entidades);
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

    private TransicaoStatusEventoInvalida transicaoInvalida(String statusAtual, String novoStatus) {
        return new TransicaoStatusEventoInvalida(statusAtual, novoStatus);
    }
}
