package com.kaikancachoeira.api.estoque.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.estoque.dto.*;
import com.kaikancachoeira.api.estoque.entity.*;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.estoque.repository.EstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.MovimentacaoEstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import com.kaikancachoeira.api.estoque.repository.TipoMovimentacaoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MovimentacaoService {

    private static final String CONSUMO_INTERNO = "CONSUMO_INTERNO";
    private static final String REUNIAO = "REUNIAO";

    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final TipoMovimentacaoRepository tipoRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final EventoRepository eventoRepository;

    public MovimentacaoService(ProdutoRepository produtoRepository,
                               EstoqueRepository estoqueRepository,
                               TipoMovimentacaoRepository tipoRepository,
                               MovimentacaoEstoqueRepository movimentacaoRepository,
                               EventoRepository eventoRepository) {
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.tipoRepository = tipoRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.eventoRepository = eventoRepository;
    }

    @Transactional(readOnly = true)
    public List<TipoMovimentacaoResponse> listarTipos() {
        return tipoRepository.findByAtivoTrueAndGeradoPeloSistemaFalseOrderByIdAsc().stream()
                .map(TipoMovimentacaoResponse::de)
                .toList();
    }

    @Transactional
    public MovimentacaoResultadoResponse movimentar(Long produtoId, MovimentarEstoqueRequest request) {
        Produto produto = produtoRepository.findByIdParaAtualizacao(produtoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", produtoId));

        TipoMovimentacao tipo = tipoRepository.findById(request.tipoMovimentacaoId())
                .filter(t -> t.getAtivo() && !t.getGeradoPeloSistema())
                .orElseThrow(() -> new RequisicaoInvalidaException("TIPO_MOVIMENTACAO_INVALIDO",
                        "Tipo de movimentação inválido."));

        validarEvento(tipo, request.eventoId());

        List<MovimentacaoEstoque> movimentacoes = tipo.getDirecao() == Direcao.ENTRADA
                ? List.of(entrada(produto, tipo, request))
                : saida(produto, tipo, request);

        return new MovimentacaoResultadoResponse(
                mapearMovimentacoesComEventoNome(movimentacoes),
                produto.getQuantidadeAtual());
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoResponse> historicoDoProduto(
            Long produtoId, Integer tipoId, Long eventoId,
            Instant dataInicio, Instant dataFim,
            int page, int size
    ) {
        if (!produtoRepository.existsById(produtoId)) {
            throw RecursoNaoEncontradoException.de("Produto", produtoId);
        }

        Instant inicio = dataInicio != null ? dataInicio : Instant.EPOCH;
        Instant fim = dataFim != null ? dataFim : Instant.parse("9999-12-31T23:59:59Z");

        Page<MovimentacaoEstoque> pagina = movimentacaoRepository.buscarPorProduto(
                produtoId, tipoId, eventoId, inicio, fim, PageRequest.of(page, size));

        return PaginaResponse.de(pagina.map(this::mapearMovimentacao));
    }

    @Transactional(readOnly = true)
    public List<EstoqueResponse> listarLotes(Long produtoId) {
        if (!produtoRepository.existsById(produtoId)) {
            throw RecursoNaoEncontradoException.de("Produto", produtoId);
        }
        return estoqueRepository.findByProdutoIdOrderByCriadoEmAscIdAsc(produtoId).stream()
                .map(EstoqueResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaginaResponse<MovimentacaoResponse> historicoDoLote(Long estoqueId, int page, int size) {
        if (!estoqueRepository.existsById(estoqueId)) {
            throw RecursoNaoEncontradoException.de("Lote", estoqueId);
        }
        Page<MovimentacaoEstoque> pagina = movimentacaoRepository.buscarPorLote(estoqueId, PageRequest.of(page, size));
        return PaginaResponse.de(pagina.map(this::mapearMovimentacao));
    }

    private void validarEvento(TipoMovimentacao tipo, Long eventoId) {
        boolean consumoInterno = CONSUMO_INTERNO.equals(tipo.getCodigo());

        if (eventoId == null) {
            if (consumoInterno) {
                throw new RequisicaoInvalidaException("EVENTO_OBRIGATORIO",
                        "Consumo interno exige um evento do tipo reunião.");
            }
            return;
        }

        String tipoEvento = movimentacaoRepository.findTipoEvento(eventoId)
                .orElseThrow(() -> new RequisicaoInvalidaException("EVENTO_INVALIDO", "Evento não encontrado."));

        if (consumoInterno && !REUNIAO.equals(tipoEvento)) {
            throw new RequisicaoInvalidaException("EVENTO_INVALIDO",
                    "Consumo interno exige um evento do tipo reunião.");
        }
    }

    private MovimentacaoEstoque entrada(Produto produto, TipoMovimentacao tipo, MovimentarEstoqueRequest request) {
        int quantidade = request.quantidade();
        BigDecimal custoTotal = BigDecimal.ZERO;
        BigDecimal custoUnitario = BigDecimal.ZERO;

        if (tipo.getExigeCusto()) {
            if (request.custoTotal() == null || request.custoTotal().signum() <= 0) {
                throw new RequisicaoInvalidaException("CUSTO_OBRIGATORIO",
                        "Custo total é obrigatório e deve ser maior que zero.");
            }
            custoTotal = request.custoTotal().setScale(2, RoundingMode.HALF_UP);
            custoUnitario = custoTotal.divide(BigDecimal.valueOf(quantidade), 2, RoundingMode.HALF_UP);
        }

        Estoque lote = new Estoque();
        lote.setProduto(produto);
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeAtual(quantidade);
        lote.setCustoUnitario(custoUnitario);
        lote.setCustoTotal(custoTotal);
        estoqueRepository.save(lote);

        produto.setQuantidadeAtual(produto.getQuantidadeAtual() + quantidade);

        return salvarMovimentacao(lote, tipo, quantidade, custoUnitario, custoTotal, request);
    }

    private List<MovimentacaoEstoque> saida(Produto produto, TipoMovimentacao tipo, MovimentarEstoqueRequest request) {
        int restante = request.quantidade();
        List<Estoque> lotes = estoqueRepository
                .findByProdutoIdAndQuantidadeAtualGreaterThanOrderByCriadoEmAscIdAsc(produto.getId(), 0);

        int saldo = lotes.stream().mapToInt(Estoque::getQuantidadeAtual).sum();
        if (saldo < restante) {
            throw new ConflitoException("SALDO_INSUFICIENTE",
                    "Saldo insuficiente. Disponível: " + saldo + ".");
        }

        List<MovimentacaoEstoque> movimentacoes = new ArrayList<>();
        for (Estoque lote : lotes) {
            if (restante == 0) {
                break;
            }
            int consumido = Math.min(restante, lote.getQuantidadeAtual());
            lote.setQuantidadeAtual(lote.getQuantidadeAtual() - consumido);
            restante -= consumido;

            BigDecimal custoTotal = lote.getCustoUnitario().multiply(BigDecimal.valueOf(consumido))
                    .setScale(2, RoundingMode.HALF_UP);

            movimentacoes.add(salvarMovimentacao(lote, tipo, -consumido, lote.getCustoUnitario(), custoTotal, request));
        }

        produto.setQuantidadeAtual(produto.getQuantidadeAtual() - request.quantidade());
        return movimentacoes;
    }

    private MovimentacaoEstoque salvarMovimentacao(Estoque lote, TipoMovimentacao tipo, int quantidade,
                                                   BigDecimal custoUnitario, BigDecimal custoTotal,
                                                   MovimentarEstoqueRequest request) {
        MovimentacaoEstoque m = new MovimentacaoEstoque();
        m.setEstoque(lote);
        m.setTipo(tipo);
        m.setQuantidade(quantidade);
        m.setCustoUnitario(custoUnitario);
        m.setCustoTotal(custoTotal);
        m.setEventoId(request.eventoId());
        m.setObservacoes(request.observacoes());
        return movimentacaoRepository.save(m);
    }

    private List<MovimentacaoResponse> mapearMovimentacoesComEventoNome(List<MovimentacaoEstoque> movimentacoes) {
        Map<Long, String> nomes = carregarNomesEventos(movimentacoes);
        return movimentacoes.stream()
                .map(m -> MovimentacaoResponse.de(m, nomes.get(m.getEventoId())))
                .toList();
    }

    private MovimentacaoResponse mapearMovimentacao(MovimentacaoEstoque movimentacao) {
        String eventoNome = null;
        if (movimentacao.getEventoId() != null) {
            eventoNome = eventoRepository.findById(movimentacao.getEventoId())
                    .map(Evento::getNome)
                    .orElse(null);
        }
        return MovimentacaoResponse.de(movimentacao, eventoNome);
    }

    private Map<Long, String> carregarNomesEventos(List<MovimentacaoEstoque> movimentacoes) {
        Set<Long> ids = movimentacoes.stream()
                .map(MovimentacaoEstoque::getEventoId)
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());

        Map<Long, String> nomes = new HashMap<>();
        if (ids.isEmpty()) {
            return nomes;
        }

        eventoRepository.findAllById(ids)
                .forEach(evento -> nomes.put(evento.getId(), evento.getNome()));

        return nomes;
    }
}
