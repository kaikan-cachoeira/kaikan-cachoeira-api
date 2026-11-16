package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.CampoErro;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.dto.*;
import com.kaikancachoeira.api.evento.entity.CustoEvento;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.StatusEvento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.exception.TransicaoStatusEventoInvalida;
import com.kaikancachoeira.api.evento.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class EventoService {

    private static final String NAO_INICIADO = "NAO_INICIADO";
    private static final String EM_ANDAMENTO = "EM_ANDAMENTO";
    private static final String ENCERRADO = "ENCERRADO";

    private static final Set<String> STATUS_PERMITIDOS = Set.of(NAO_INICIADO, EM_ANDAMENTO, ENCERRADO);

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
    public PaginaResponse<EventoResponse> listar(
            String q,
            String status,
            String tipos,
            Instant dataInicio,
            Instant dataFim,
            int page,
            int size
    ) {
        validarPaginacao(page, size);

        Specification<Evento> spec = Specification.unrestricted();

        if (q != null && !q.isBlank()) {
            spec = spec.and(EventoSpecifications.nomeContem(q.trim()));
        }

        Set<String> codigosStatus = parseStatus(status);
        if (!codigosStatus.isEmpty()) {
            spec = spec.and(EventoSpecifications.comStatus(codigosStatus));
        }

        Set<TipoEvento> tiposEvento = parseTipos(tipos);
        if (!tiposEvento.isEmpty()) {
            spec = spec.and(EventoSpecifications.comTipos(tiposEvento));
        }

        if (dataInicio != null) {
            spec = spec.and(EventoSpecifications.inicioA_partirDe(dataInicio));
        }

        if (dataFim != null) {
            spec = spec.and(EventoSpecifications.inicioAte(dataFim));
        }

        spec = spec.and(EventoSpecifications.ordenarComEmAndamentoPrimeiro());

        Pageable pageable = PageRequest.of(page, size);

        Page<Evento> pagina = eventoRepository.findAll(spec, pageable);

        Page<EventoResponse> paginaDto = pagina.map(EventoResponse::de);

        return PaginaResponse.de(paginaDto);
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

    private Set<String> parseStatus(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        Set<String> codigos = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(token -> !token.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));

        if (codigos.isEmpty()) {
            return Set.of();
        }

        boolean invalido = codigos.stream().anyMatch(token -> !STATUS_PERMITIDOS.contains(token));
        if (invalido) {
            throw parametroInvalido("status");
        }

        return codigos;
    }

    private Set<TipoEvento> parseTipos(String csv) {
        if (csv == null || csv.isBlank()) {
            return Set.of();
        }

        Set<TipoEvento> tipos = new LinkedHashSet<>();
        for (String token : csv.split(",")) {
            String normalizado = token.trim();
            if (normalizado.isBlank()) {
                continue;
            }

            try {
                tipos.add(TipoEvento.valueOf(normalizado));
            } catch (IllegalArgumentException ex) {
                throw parametroInvalido("tipos");
            }
        }

        return tipos;
    }

    private void validarPaginacao(int page, int size) {
        if (page < 0) {
            throw validacao("page", "deve ser maior ou igual a 0");
        }
        if (size < 1 || size > 100) {
            throw validacao("size", "deve ser entre 1 e 100");
        }
    }

    private RequisicaoInvalidaException parametroInvalido(String campo) {
        return new RequisicaoInvalidaException(
                "REQUISICAO_INVALIDA",
                "Valor inválido para o parâmetro",
                List.of(new CampoErro(campo, "Valor inválido"))
        );
    }

    private ValidacaoException validacao(String campo, String mensagem) {
        return new ValidacaoException(
                "Dados inválidos",
                List.of(new CampoErro(campo, mensagem))
        );
    }
}
