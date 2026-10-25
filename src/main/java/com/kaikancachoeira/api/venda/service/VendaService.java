package com.kaikancachoeira.api.venda.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.estoque.entity.Estoque;
import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;
import com.kaikancachoeira.api.estoque.entity.TipoMovimentacao;
import com.kaikancachoeira.api.estoque.repository.EstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.MovimentacaoEstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import com.kaikancachoeira.api.estoque.repository.TipoMovimentacaoRepository;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.repository.ProdutoEventoRepository;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import com.kaikancachoeira.api.venda.dto.ItemVendaRequest;
import com.kaikancachoeira.api.venda.dto.VendaRequest;
import com.kaikancachoeira.api.venda.dto.VendaResponse;
import com.kaikancachoeira.api.venda.entity.ItemVenda;
import com.kaikancachoeira.api.venda.entity.Venda;
import com.kaikancachoeira.api.venda.repository.VendaRepository;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.venda.dto.ItemVendaDetalheResponse;
import com.kaikancachoeira.api.venda.dto.VendaDetalheResponse;
import com.kaikancachoeira.api.venda.dto.VendaHistoricoResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class VendaService {

    private static final String TIPO_VENDA = "VENDA";
    private static final String TIPO_ESTORNO_VENDA = "ESTORNO_VENDA";

    private final EventoRepository eventoRepository;
    private final EventoGuard eventoGuard;
    private final ProdutoEventoRepository produtoEventoRepository;
    private final VendaRepository vendaRepository;
    private final EstoqueRepository estoqueRepository;
    private final MovimentacaoEstoqueRepository movimentacaoRepository;
    private final TipoMovimentacaoRepository tipoMovimentacaoRepository;
    private final ProdutoRepository produtoRepository;
    private final Clock clock;

    public VendaService(EventoRepository eventoRepository,
                        EventoGuard eventoGuard,
                        ProdutoEventoRepository produtoEventoRepository,
                        VendaRepository vendaRepository,
                        EstoqueRepository estoqueRepository,
                        MovimentacaoEstoqueRepository movimentacaoRepository,
                        TipoMovimentacaoRepository tipoMovimentacaoRepository,
                        ProdutoRepository produtoRepository,
                        Clock clock) {
        this.eventoRepository = eventoRepository;
        this.eventoGuard = eventoGuard;
        this.produtoEventoRepository = produtoEventoRepository;
        this.vendaRepository = vendaRepository;
        this.estoqueRepository = estoqueRepository;
        this.movimentacaoRepository = movimentacaoRepository;
        this.tipoMovimentacaoRepository = tipoMovimentacaoRepository;
        this.produtoRepository = produtoRepository;
        this.clock = clock;
    }

    @Transactional
    public VendaResponse registrar(Long eventoId, VendaRequest request) {
        Evento evento = eventoRepository.findByIdParaAtualizacao(eventoId)
                .filter(e -> e.getTipoEvento() == TipoEvento.EVENTO
                        && EventoGuard.EM_ANDAMENTO.equals(e.getStatus().getCodigo()))
                .orElseThrow(() -> new ConflitoException("EVENTO_NAO_EM_ANDAMENTO",
                        "O evento não existe, não é do tipo EVENTO ou não está em andamento."));

        Set<Long> ids = new HashSet<>();
        for (ItemVendaRequest item : request.itens()) {
            if (!ids.add(item.produtoEventoId())) {
                throw ValidacaoException.campo("itens", "Produto repetido na venda");
            }
        }

        Map<Long, ProdutoEvento> produtos = produtoEventoRepository.findByEventoIdAndIdIn(eventoId, ids).stream()
                .collect(Collectors.toMap(ProdutoEvento::getId, Function.identity()));
        if (produtos.size() != ids.size()) {
            throw ValidacaoException.campo("itens", "Há produtos que não pertencem ao evento");
        }

        Venda venda = new Venda();
        venda.setEvento(evento);

        BigDecimal total = BigDecimal.ZERO;
        for (ItemVendaRequest itemRequest : request.itens()) {
            ProdutoEvento produto = produtos.get(itemRequest.produtoEventoId());
            ItemVenda item = new ItemVenda();
            item.setProdutoEvento(produto);
            item.setQuantidade(itemRequest.quantidade());
            item.setPrecoUnitario(produto.getPrecoVenda());
            item.setDescontoUnitario(produto.getDesconto());
            venda.adicionarItem(item);
            total = total.add(item.getSubtotal());
        }

        BigDecimal valorFinal = request.valorFinal() == null ? total : request.valorFinal();
        if (valorFinal.compareTo(total) > 0) {
            throw ValidacaoException.campo("valorFinal", "Valor final não pode ser maior que o total da venda");
        }

        venda.setDesconto(total.subtract(valorFinal).setScale(2, RoundingMode.HALF_UP));
        venda.setPrecoTotal(valorFinal.setScale(2, RoundingMode.HALF_UP));
        vendaRepository.saveAndFlush(venda);

        baixarEstoque(venda, eventoId);
        return VendaResponse.de(venda);
    }

    @Transactional
    public void cancelar(Long eventoId, Long vendaId) {
        Evento evento = eventoGuard.buscarComLock(eventoId);
        Venda venda = vendaRepository.findByIdAndEventoId(vendaId, eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Venda", vendaId));

        if (venda.cancelada()) {
            throw new ConflitoException("VENDA_JA_CANCELADA", "A venda já está cancelada.");
        }
        if (EventoGuard.ENCERRADO.equals(evento.getStatus().getCodigo())) {
            throw new ConflitoException("EVENTO_ENCERRADO",
                    "Não é possível cancelar venda de evento encerrado.");
        }

        Instant agora = Instant.now(clock);
        venda.setCanceladaEm(agora);
        estornarEstoque(venda, eventoId, agora);
    }

    @Transactional(readOnly = true)
    public PaginaResponse<VendaHistoricoResponse> listarHistorico(
            Long eventoId,
            String q,
            Instant dataHoraInicio,
            Instant dataHoraFim,
            Boolean cancelada,
            int page,
            int size
    ) {
        if (!eventoRepository.existsById(eventoId)) {
            throw RecursoNaoEncontradoException.de("Evento", eventoId);
        }

        String texto = (q == null || q.isBlank()) ? null : q.trim();

        Page<Venda> pagina = vendaRepository.buscarHistorico(
                eventoId,
                texto,
                dataHoraInicio,
                dataHoraFim,
                cancelada,
                PageRequest.of(page, size)
        );

        return PaginaResponse.de(pagina.map(VendaHistoricoResponse::de));
    }

    @Transactional(readOnly = true)
    public VendaDetalheResponse detalhar(Long eventoId, Long vendaId) {
        Venda venda = vendaRepository.findByIdAndEventoId(vendaId, eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Venda", vendaId));

        return VendaDetalheResponse.de(venda);
    }

    private void baixarEstoque(Venda venda, Long eventoId) {
        TipoMovimentacao tipoVenda = tipo(TIPO_VENDA);

        List<ItemVenda> itens = venda.getItens().stream()
                .filter(i -> i.getProdutoEvento().getOrigem() == OrigemProdutoEvento.ESTOQUE)
                .sorted(Comparator.comparing((ItemVenda i) -> i.getProdutoEvento().getProduto().getId()))
                .toList();

        Instant agora = Instant.now(clock);
        for (ItemVenda item : itens) {
            Long produtoId = item.getProdutoEvento().getProduto().getId();
            int quantidade = item.getQuantidade();

            List<Estoque> lotes = estoqueRepository.lotesDisponiveisParaBaixa(produtoId);
            int saldo = lotes.stream().mapToInt(Estoque::getQuantidadeAtual).sum();
            if (saldo < quantidade) {
                throw new ConflitoException("SALDO_INSUFICIENTE",
                        "Saldo insuficiente de " + item.getProdutoEvento().getNome() + ". Disponível: " + saldo + ".");
            }

            int restante = quantidade;
            for (Estoque lote : lotes) {
                if (restante == 0) {
                    break;
                }
                int consumido = Math.min(restante, lote.getQuantidadeAtual());
                lote.setQuantidadeAtual(lote.getQuantidadeAtual() - consumido);
                restante -= consumido;

                BigDecimal custoTotal = lote.getCustoUnitario().multiply(BigDecimal.valueOf(consumido))
                        .setScale(2, RoundingMode.HALF_UP);
                movimentacaoRepository.save(movimentacao(lote, tipoVenda, eventoId, item.getId(),
                        -consumido, lote.getCustoUnitario(), custoTotal));
            }

            produtoRepository.ajustarQuantidade(produtoId, -quantidade, agora);
        }
    }

    private void estornarEstoque(Venda venda, Long eventoId, Instant agora) {
        List<Long> itemIds = venda.getItens().stream().map(ItemVenda::getId).toList();
        List<MovimentacaoEstoque> saidas = movimentacaoRepository.buscarPorItensVendaETipo(itemIds, TIPO_VENDA);
        if (saidas.isEmpty()) {
            return;
        }

        Set<Long> loteIds = saidas.stream()
                .map(m -> m.getEstoque().getId())
                .collect(Collectors.toCollection(TreeSet::new));
        Map<Long, Estoque> lotes = estoqueRepository.travarPorIds(loteIds).stream()
                .collect(Collectors.toMap(Estoque::getId, Function.identity()));

        TipoMovimentacao tipoEstorno = tipo(TIPO_ESTORNO_VENDA);
        Map<Long, Integer> devolvidoPorProduto = new TreeMap<>();

        for (MovimentacaoEstoque saida : saidas) {
            Estoque lote = lotes.get(saida.getEstoque().getId());
            int quantidade = -saida.getQuantidade();
            lote.setQuantidadeAtual(lote.getQuantidadeAtual() + quantidade);

            movimentacaoRepository.save(movimentacao(lote, tipoEstorno, eventoId, saida.getItemVendaId(),
                    quantidade, saida.getCustoUnitario(), saida.getCustoTotal()));
            devolvidoPorProduto.merge(lote.getProduto().getId(), quantidade, Integer::sum);
        }

        devolvidoPorProduto.forEach((produtoId, quantidade) ->
                produtoRepository.ajustarQuantidade(produtoId, quantidade, agora));
    }

    private MovimentacaoEstoque movimentacao(Estoque lote, TipoMovimentacao tipo, Long eventoId, Long itemVendaId,
                                             int quantidade, BigDecimal custoUnitario, BigDecimal custoTotal) {
        MovimentacaoEstoque m = new MovimentacaoEstoque();
        m.setEstoque(lote);
        m.setTipo(tipo);
        m.setEventoId(eventoId);
        m.setItemVendaId(itemVendaId);
        m.setQuantidade(quantidade);
        m.setCustoUnitario(custoUnitario);
        m.setCustoTotal(custoTotal);
        return m;
    }

    private TipoMovimentacao tipo(String codigo) {
        return tipoMovimentacaoRepository.findByCodigo(codigo)
                .orElseThrow(() -> new IllegalStateException("Tipo de movimentação não cadastrado: " + codigo));
    }
}
