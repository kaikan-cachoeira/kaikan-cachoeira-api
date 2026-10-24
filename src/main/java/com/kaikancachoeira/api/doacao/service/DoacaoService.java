package com.kaikancachoeira.api.doacao.service;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.CampoErro;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.doacao.dto.DoacaoRequest;
import com.kaikancachoeira.api.doacao.dto.DoacaoResponse;
import com.kaikancachoeira.api.doacao.entity.Doacao;
import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import com.kaikancachoeira.api.doacao.repository.DoacaoRepository;
import com.kaikancachoeira.api.doacao.repository.DoacaoSpecifications;
import com.kaikancachoeira.api.estoque.entity.Estoque;
import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;
import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.estoque.entity.TipoMovimentacao;
import com.kaikancachoeira.api.estoque.repository.EstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.MovimentacaoEstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import com.kaikancachoeira.api.estoque.repository.TipoMovimentacaoRepository;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Service
public class DoacaoService {

    private static final String ENCERRADO = "ENCERRADO";
    private static final String CODIGO_DOACAO = "DOACAO";

    private final DoacaoRepository doacaoRepository;
    private final EventoRepository eventoRepository;
    private final ProdutoRepository produtoRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final TipoMovimentacaoRepository tipoMovimentacaoRepository;

    public DoacaoService(DoacaoRepository doacaoRepository,
                         EventoRepository eventoRepository,
                         ProdutoRepository produtoRepository,
                         EstoqueRepository estoqueRepository,
                         MovimentacaoEstoqueRepository movimentacaoRepository,
                         TipoMovimentacaoRepository tipoMovimentacaoRepository) {
        this.doacaoRepository = doacaoRepository;
        this.eventoRepository = eventoRepository;
        this.produtoRepository = produtoRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.tipoMovimentacaoRepository = tipoMovimentacaoRepository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<DoacaoResponse> listar(String q, List<String> tipos, List<Long> eventoIds,
                                                 int page, int size) {
        Specification<Doacao> spec = Specification.unrestricted();

        if (q != null && !q.isBlank()) {
            spec = spec.and(DoacaoSpecifications.textoContem(q.trim()));
        }

        List<TipoDoacao> tiposFiltro = converterTipos(tipos);
        if (!tiposFiltro.isEmpty()) {
            spec = spec.and(DoacaoSpecifications.comTipos(tiposFiltro));
        }

        if (eventoIds != null && !eventoIds.isEmpty()) {
            spec = spec.and(DoacaoSpecifications.doEventos(eventoIds));
        }

        PageRequest paginacao = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")));

        Page<Doacao> pagina = doacaoRepository.findAll(spec, paginacao);
        return PaginaResponse.de(pagina.map(DoacaoResponse::de));
    }

    @Transactional
    public DoacaoResponse criar(DoacaoRequest request) {
        TipoDoacao tipo = TipoDoacao.de(request.tipo())
                .orElseThrow(() -> ValidacaoException.campo("tipo", mensagemTipoInvalido()));

        List<CampoErro> erros = new ArrayList<>();
        validarCamposDoTipo(tipo, request, true, erros);

        Evento evento = eventoRepository.findByIdParaAtualizacao(request.eventoId()).orElse(null);
        if (evento == null) {
            erros.add(new CampoErro("eventoId", "Evento não encontrado"));
        }

        Produto produto = null;
        if (tipo == TipoDoacao.REABASTECIMENTO && request.produtoId() != null) {
            produto = produtoRepository.findByIdParaAtualizacao(request.produtoId()).orElse(null);
            if (produto == null) {
                erros.add(new CampoErro("produtoId", "Produto não encontrado ou inativo"));
            }
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException("Dados inválidos", erros);
        }

        if (ENCERRADO.equals(evento.getStatus().getCodigo())) {
            throw new ConflitoException("EVENTO_ENCERRADO",
                    "Não é possível registrar doação em um evento encerrado.");
        }

        Doacao doacao = new Doacao();
        doacao.setTipo(tipo);
        aplicarDados(doacao, tipo, request, produto);
        doacaoRepository.save(doacao);

        if (tipo == TipoDoacao.REABASTECIMENTO) {
            gerarEntradaDeEstoque(doacao, produto);
        }

        return DoacaoResponse.de(doacao);
    }

    @Transactional
    public void atualizar(Long id, DoacaoRequest request) {
        Doacao doacao = doacaoRepository.findByIdParaAtualizacao(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Doação", id));

        TipoDoacao tipoInformado = TipoDoacao.de(request.tipo())
                .orElseThrow(() -> ValidacaoException.campo("tipo", mensagemTipoInvalido()));

        List<CampoErro> erros = new ArrayList<>();

        if (tipoInformado != doacao.getTipo()) {
            erros.add(new CampoErro("tipo", "O tipo da doação não pode ser alterado"));
        }

        if (tipoInformado == doacao.getTipo()) {
            validarCamposDoTipo(doacao.getTipo(), request, false, erros);
        }

        if (!eventoRepository.existsById(request.eventoId())) {
            erros.add(new CampoErro("eventoId", "Evento não encontrado"));
        }

        if (doacao.getTipo() == TipoDoacao.REABASTECIMENTO
                && request.produtoId() != null
                && !request.produtoId().equals(doacao.getProdutoId())) {
            erros.add(new CampoErro("produtoId", "O produto da doação não pode ser alterado"));
        }

        if (!erros.isEmpty()) {
            throw new ValidacaoException("Dados inválidos", erros);
        }

        if (doacao.getTipo() == TipoDoacao.REABASTECIMENTO) {
            ajustarEstoque(doacao, request);
        }

        aplicarDados(doacao, doacao.getTipo(), request, null);
    }

    private void validarCamposDoTipo(TipoDoacao tipo, DoacaoRequest request, boolean criacao,
                                     List<CampoErro> erros) {
        switch (tipo) {
            case DINHEIRO -> {
                if (request.valor() == null || request.valor().signum() <= 0) {
                    erros.add(new CampoErro("valor", "Informe um valor maior que zero"));
                }
                if (FormaPagamentoDoacao.de(request.formaPagamento()).isEmpty()) {
                    erros.add(new CampoErro("formaPagamento",
                            "Informe a forma de pagamento: PIX, DEBITO, CREDITO ou DINHEIRO"));
                }
            }
            case PRODUTO -> {
                if (request.nome() == null || request.nome().isBlank()) {
                    erros.add(new CampoErro("nome", "Informe a descrição do item"));
                }
            }
            case REABASTECIMENTO -> {
                if (criacao && request.produtoId() == null) {
                    erros.add(new CampoErro("produtoId", "Informe o produto"));
                }
                if (request.quantidade() == null || request.quantidade() <= 0) {
                    erros.add(new CampoErro("quantidade", "Informe uma quantidade maior que zero"));
                }
            }
        }
    }

    private void aplicarDados(Doacao doacao, TipoDoacao tipo, DoacaoRequest request, Produto produto) {
        doacao.setEventoId(request.eventoId());
        doacao.setDoador(request.doador().trim());
        doacao.setValorFicha(escala(request.valorFicha()));

        switch (tipo) {
            case DINHEIRO -> {
                doacao.setValor(escala(request.valor()));
                doacao.setFormaPagamento(FormaPagamentoDoacao.de(request.formaPagamento()).orElseThrow());
                doacao.setProdutoId(null);
                doacao.setNome(null);
                doacao.setQuantidade(null);
            }
            case PRODUTO -> {
                doacao.setNome(request.nome().trim());
                doacao.setValor(null);
                doacao.setFormaPagamento(null);
                doacao.setProdutoId(null);
                doacao.setQuantidade(null);
            }
            case REABASTECIMENTO -> {
                if (produto != null) {
                    doacao.setProdutoId(produto.getId());
                    doacao.setNome(produto.getNome());
                }
                doacao.setQuantidade(request.quantidade());
                doacao.setValor(null);
                doacao.setFormaPagamento(null);
            }
        }
    }

    private void gerarEntradaDeEstoque(Doacao doacao, Produto produto) {
        int quantidade = doacao.getQuantidade();

        TipoMovimentacao tipoDoacao = tipoMovimentacaoRepository.findByCodigo(CODIGO_DOACAO)
                .orElseThrow(() -> new IllegalStateException("Tipo de movimentação DOACAO não cadastrado"));

        Estoque lote = new Estoque();
        lote.setProduto(produto);
        lote.setQuantidadeInicial(quantidade);
        lote.setQuantidadeAtual(quantidade);
        lote.setCustoUnitario(BigDecimal.ZERO);
        lote.setCustoTotal(BigDecimal.ZERO);
        estoqueRepository.save(lote);

        MovimentacaoEstoque movimentacao = new MovimentacaoEstoque();
        movimentacao.setEstoque(lote);
        movimentacao.setTipo(tipoDoacao);
        movimentacao.setQuantidade(quantidade);
        movimentacao.setCustoUnitario(BigDecimal.ZERO);
        movimentacao.setCustoTotal(BigDecimal.ZERO);
        movimentacao.setEventoId(doacao.getEventoId());
        movimentacao.setDoacaoId(doacao.getId());
        movimentacaoRepository.save(movimentacao);

        produto.setQuantidadeAtual(produto.getQuantidadeAtual() + quantidade);
    }

    private void ajustarEstoque(Doacao doacao, DoacaoRequest request) {
        int antiga = doacao.getQuantidade();
        int nova = request.quantidade();

        MovimentacaoEstoque movimentacao = doacaoRepository.movimentacaoDaDoacao(doacao.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Movimentação DOACAO não encontrada para a doação " + doacao.getId()));

        if (nova != antiga) {
            Produto produto = produtoRepository.findByIdParaAtualizacao(doacao.getProdutoId())
                    .orElseThrow(() -> new ConflitoException("PRODUTO_INATIVO",
                            "O produto da doação está inativo."));

            Estoque lote = doacaoRepository.lotePorDoacaoParaAtualizacao(doacao.getId())
                    .orElseThrow(() -> new IllegalStateException(
                            "Lote não encontrado para a doação " + doacao.getId()));

            int consumido = lote.getQuantidadeInicial() - lote.getQuantidadeAtual();
            if (nova < consumido) {
                throw new ConflitoException("QUANTIDADE_ABAIXO_DO_CONSUMIDO",
                        "A quantidade não pode ser menor que a já consumida do lote (" + consumido + ").");
            }

            lote.setQuantidadeInicial(nova);
            lote.setQuantidadeAtual(nova - consumido);
            produto.setQuantidadeAtual(produto.getQuantidadeAtual() + (nova - antiga));
            movimentacao.setQuantidade(nova);
        }

        movimentacao.setEventoId(request.eventoId());
    }

    private List<TipoDoacao> converterTipos(List<String> tipos) {
        if (tipos == null || tipos.isEmpty()) {
            return List.of();
        }
        List<TipoDoacao> convertidos = new ArrayList<>();
        for (String tipo : tipos) {
            convertidos.add(TipoDoacao.de(tipo)
                    .orElseThrow(() -> ValidacaoException.campo("tipo", mensagemTipoInvalido())));
        }
        return convertidos;
    }

    private String mensagemTipoInvalido() {
        return "O tipo deve ser dinheiro, produto ou reabastecimento";
    }

    private BigDecimal escala(BigDecimal valor) {
        return valor == null ? null : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
