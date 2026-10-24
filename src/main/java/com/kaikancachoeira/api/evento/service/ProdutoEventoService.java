package com.kaikancachoeira.api.evento.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.estoque.repository.CategoriaRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import com.kaikancachoeira.api.evento.dto.FotoEventoResponse;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoRequest;
import com.kaikancachoeira.api.evento.dto.ProdutoEventoResponse;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.entity.ProdutoEvento;
import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import com.kaikancachoeira.api.evento.enums.TipoEvento;
import com.kaikancachoeira.api.evento.repository.EventoRepository;
import com.kaikancachoeira.api.evento.repository.ProdutoEventoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
public class ProdutoEventoService {

    private static final String NAO_INICIADO = "NAO_INICIADO";

    private final EventoRepository eventoRepository;
    private final ProdutoEventoRepository produtoEventoRepository;
    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final ArmazenamentoService armazenamentoService;

    public ProdutoEventoService(EventoRepository eventoRepository,
                                ProdutoEventoRepository produtoEventoRepository,
                                ProdutoRepository produtoRepository,
                                CategoriaRepository categoriaRepository,
                                ArmazenamentoService armazenamentoService) {
        this.eventoRepository = eventoRepository;
        this.produtoEventoRepository = produtoEventoRepository;
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.armazenamentoService = armazenamentoService;
    }

    @Transactional(readOnly = true)
    public List<ProdutoEventoResponse> listar(Long eventoId, String q, List<String> categorias) {
        buscarEvento(eventoId);
        String texto = (q == null) ? "" : q.trim();
        List<String> cats = (categorias == null || categorias.isEmpty()) ? null : categorias;

        return produtoEventoRepository.buscarPorEvento(eventoId, texto, cats).stream()
                .map(pe -> ProdutoEventoResponse.de(pe,
                        produtoEventoRepository.quantidadeVendida(pe.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> listarCategorias(Long eventoId) {
        buscarEvento(eventoId);
        return produtoEventoRepository.findCategoriasDistintas(eventoId);
    }

    @Transactional
    public ProdutoEventoResponse adicionar(Long eventoId, ProdutoEventoRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarNaoIniciado(evento, "adicionar produtos ao cardápio");
        validarNaoReuniao(evento);

        ProdutoEvento pe = new ProdutoEvento();
        pe.setEvento(evento);

        if (OrigemProdutoEvento.ESTOQUE.equals(request.origem())) {
            preencherEstoque(pe, eventoId, request);
        } else if (OrigemProdutoEvento.PRODUZIDO.equals(request.origem())) {
            preencherProduzido(pe, eventoId, request, null);
        } else {
            throw new RequisicaoInvalidaException("ORIGEM_INVALIDA", "Origem deve ser ESTOQUE ou PRODUZIDO.");
        }

        ProdutoEvento salvo = produtoEventoRepository.save(pe);
        return ProdutoEventoResponse.de(salvo, 0);
    }

    @Transactional
    public FotoEventoResponse atualizarFoto(Long eventoId, Long produtoEventoId, MultipartFile arquivo) {
        buscarEvento(eventoId);
        ProdutoEvento pe = buscarItem(eventoId, produtoEventoId);

        String chaveAntiga = pe.getFotoUrl();
        String chave = armazenamentoService.salvarImagem(arquivo);
        pe.setFotoUrl(chave);
        armazenamentoService.remover(chaveAntiga);

        return new FotoEventoResponse(ArmazenamentoService.url(chave));
    }

    @Transactional
    public ProdutoEventoResponse atualizar(Long eventoId, Long produtoEventoId, ProdutoEventoRequest request) {
        Evento evento = buscarEvento(eventoId);
        validarNaoIniciado(evento, "editar o cardápio");
        ProdutoEvento pe = buscarItem(eventoId, produtoEventoId);

        if (OrigemProdutoEvento.ESTOQUE.equals(pe.getOrigem())) {
            pe.setPrecoVenda(request.precoVenda());
            if (request.quantidadePrevista() == null || request.quantidadePrevista() <= 0) {
                throw new RequisicaoInvalidaException("QUANTIDADE_INVALIDA", "Quantidade prevista deve ser maior que 0.");
            }
            pe.setQuantidadePrevista(request.quantidadePrevista());
        } else {
            preencherProduzido(pe, eventoId, request, produtoEventoId);
        }

        return ProdutoEventoResponse.de(pe,
                produtoEventoRepository.quantidadeVendida(pe.getId()));
    }

    @Transactional
    public void remover(Long eventoId, Long produtoEventoId) {
        Evento evento = buscarEvento(eventoId);
        validarNaoIniciado(evento, "remover produtos do cardápio");
        ProdutoEvento pe = buscarItem(eventoId, produtoEventoId);

        if (produtoEventoRepository.foiVendido(pe.getId())) {
            throw new ConflitoException("PRODUTO_JA_VENDIDO",
                    "Não é possível remover um produto que já foi vendido.");
        }

        produtoEventoRepository.delete(pe);
    }

    private Evento buscarEvento(Long eventoId) {
        return eventoRepository.findById(eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Evento", eventoId));
    }

    private ProdutoEvento buscarItem(Long eventoId, Long id) {
        return produtoEventoRepository.findByIdAndEventoId(id, eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto do evento", id));
    }

    private void validarNaoIniciado(Evento evento, String acao) {
        if (!NAO_INICIADO.equals(evento.getStatus().getCodigo())) {
            throw new ConflitoException("EVENTO_JA_INICIADO",
                    "Só é possível " + acao + " com o evento no status NAO_INICIADO.");
        }
    }

    private void validarNaoReuniao(Evento evento) {
        if (TipoEvento.REUNIAO.equals(evento.getTipoEvento())) {
            throw new ConflitoException("REUNIAO_SEM_CARDAPIO",
                    "Reuniões não possuem cardápio.");
        }
    }

    private void preencherEstoque(ProdutoEvento pe, Long eventoId, ProdutoEventoRequest request) {
        if (request.produtoId() == null) {
            throw new RequisicaoInvalidaException("PRODUTO_OBRIGATORIO",
                    "produtoId é obrigatório para itens do estoque.");
        }

        if (request.quantidadePrevista() == null || request.quantidadePrevista() <= 0) {
            throw new RequisicaoInvalidaException("QUANTIDADE_INVALIDA",
                    "quantidadePrevista é obrigatória e deve ser maior que 0 para itens do estoque.");
        }

        if (request.precoVenda() == null) {
            throw new RequisicaoInvalidaException("PRECO_OBRIGATORIO", "precoVenda é obrigatório.");
        }

        Produto produto = produtoRepository.findByIdAndAtivoTrue(request.produtoId())
                .orElseThrow(() -> new RequisicaoInvalidaException("PRODUTO_INVALIDO",
                        "Produto não encontrado ou inativo."));

        if (produtoEventoRepository.existsByEventoIdAndProdutoId(eventoId, produto.getId())) {
            throw new ConflitoException("PRODUTO_JA_NO_EVENTO",
                    "Este produto já está no cardápio do evento.");
        }

        pe.setOrigem(OrigemProdutoEvento.ESTOQUE);
        pe.setProduto(produto);
        pe.setPrecoVenda(request.precoVenda());
        pe.setQuantidadePrevista(request.quantidadePrevista());
        pe.setCustoTotalIngredientes(null);
        pe.setNome(produto.getNome());
        pe.setDescricao(produto.getDescricao());
        pe.setCategoria(produto.getCategoria() != null ? produto.getCategoria().getNome() : null);
        pe.setFotoUrl(produto.getFotoUrl());
    }

    private void preencherProduzido(ProdutoEvento pe, Long eventoId, ProdutoEventoRequest request, Long idAtual) {
        if (request.nome() == null || request.nome().isBlank()) {
            throw new RequisicaoInvalidaException("NOME_OBRIGATORIO",
                    "nome é obrigatório para itens produzidos no evento.");
        }

        if (request.precoVenda() == null) {
            throw new RequisicaoInvalidaException("PRECO_OBRIGATORIO", "precoVenda é obrigatório.");
        }

        if (request.custoTotalIngredientes() == null) {
            throw new RequisicaoInvalidaException("CUSTO_OBRIGATORIO",
                    "custoTotalIngredientes é obrigatório para itens produzidos no evento.");
        }

        boolean nomeExiste = (idAtual == null)
                ? produtoEventoRepository.existsByEventoIdAndNomeIgnoreCase(eventoId, request.nome())
                : produtoEventoRepository.existsByEventoIdAndNomeIgnoreCaseAndIdNot(eventoId, request.nome(), idAtual);

        if (nomeExiste) {
            throw new ConflitoException("NOME_DUPLICADO",
                    "Já existe um item com este nome no cardápio deste evento.");
        }

        if (request.categoria() != null && !request.categoria().isBlank()) {
            if (!categoriaRepository.existsByNomeIgnoreCase(request.categoria())) {
                throw new RequisicaoInvalidaException("CATEGORIA_INVALIDA",
                        "Categoria não encontrada: " + request.categoria());
            }
        }

        pe.setOrigem(OrigemProdutoEvento.PRODUZIDO);
        pe.setProduto(null);
        pe.setPrecoVenda(request.precoVenda());
        pe.setQuantidadePrevista(null);
        pe.setCustoTotalIngredientes(request.custoTotalIngredientes());
        pe.setNome(request.nome().trim());
        pe.setDescricao(request.descricao());
        pe.setCategoria(request.categoria());
    }
}