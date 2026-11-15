package com.kaikancachoeira.api.estoque.service;

import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import com.kaikancachoeira.api.estoque.entity.Categoria;
import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.estoque.enums.SituacaoProduto;
import com.kaikancachoeira.api.estoque.repository.ProdutoSpecifications;
import com.kaikancachoeira.api.estoque.repository.CategoriaRepository;
import com.kaikancachoeira.api.estoque.repository.EstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;
    private final CategoriaRepository categoriaRepository;
    private final EstoqueRepository estoqueRepository;
    private final ArmazenamentoService armazenamentoService;

    public ProdutoService(ProdutoRepository produtoRepository,
                          CategoriaRepository categoriaRepository,
                          EstoqueRepository estoqueRepository,
                          ArmazenamentoService armazenamentoService) {
        this.produtoRepository = produtoRepository;
        this.categoriaRepository = categoriaRepository;
        this.estoqueRepository = estoqueRepository;
        this.armazenamentoService = armazenamentoService;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<ProdutoResponse> listar(String q, List<Integer> categoriaIds, String situacao, int page, int size) {
        String texto = q == null ? "" : q.trim();
        SituacaoProduto situacaoProduto = SituacaoProduto.fromCodigo(situacao);

        Specification<Produto> spec = Specification.where(ProdutoSpecifications.ativos())
                .and(ProdutoSpecifications.carregarCategoria());

        if (!texto.isBlank()) {
            spec = spec.and(ProdutoSpecifications.textoContem(texto));
        }

        if (categoriaIds != null && !categoriaIds.isEmpty()) {
            spec = spec.and(ProdutoSpecifications.categoriasEm(categoriaIds.stream().filter(java.util.Objects::nonNull).toList()));
        }

        if (situacaoProduto != null) {
            spec = spec.and(ProdutoSpecifications.situacao(situacaoProduto));
        }

        Page<Produto> pagina = produtoRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Order.asc("nome"), Sort.Order.asc("id")))
        );

        return PaginaResponse.de(pagina.map(ProdutoResponse::de));
    }

    @Transactional(readOnly = true)
    public Optional<ProdutoResponse> buscarPorId(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id).map(ProdutoResponse::de);
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        return criar(request, null);
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request, MultipartFile foto) {
        boolean temFoto = foto != null && !foto.isEmpty();
        if (temFoto) {
            armazenamentoService.validarImagem(foto);
        }

        String nome = request.nome().trim();

        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw produtoDuplicado();
        }

        Produto produto = new Produto();
        preencher(produto, nome, request);

        if (temFoto) {
            String chave = armazenamentoService.salvarImagem(foto);
            removerArquivoSeNaoConfirmar(chave);
            produto.setFotoUrl(chave);
        }

        return ProdutoResponse.de(produtoRepository.save(produto));
    }

    @Transactional
    public void atualizar(Long id, ProdutoRequest request) {
        Produto produto = buscarAtivo(id);
        String nome = request.nome().trim();

        if (produtoRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw produtoDuplicado();
        }

        preencher(produto, nome, request);
    }

    @Transactional
    public FotoResponse atualizarFoto(Long id, MultipartFile arquivo) {
        Produto produto = buscarAtivo(id);

        String chaveAntiga = produto.getFotoUrl();
        String chave = armazenamentoService.salvarImagem(arquivo);
        produto.setFotoUrl(chave);
        armazenamentoService.remover(chaveAntiga);

        return new FotoResponse(ArmazenamentoService.url(chave));
    }

    @Transactional
    public boolean excluir(Long id) {
        Optional<Produto> encontrado = produtoRepository.findByIdAndAtivoTrue(id);
        if (encontrado.isEmpty()) {
            return false;
        }

        Produto produto = encontrado.get();
        boolean temHistorico = estoqueRepository.existsByProdutoId(id)
                || produtoRepository.possuiVinculoComEventoOuDoacao(id);

        if (temHistorico) {
            produto.setAtivo(false);
        } else {
            produtoRepository.delete(produto);
        }
        return true;
    }

    private void removerArquivoSeNaoConfirmar(String chave) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    armazenamentoService.remover(chave);
                }
            }
        });
    }

    private Produto buscarAtivo(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Produto", id));
    }

    private void preencher(Produto produto, String nome, ProdutoRequest request) {
        produto.setNome(nome);
        produto.setDescricao(request.descricao());
        produto.setEstoqueMinimo(request.estoqueMinimo());
        produto.setCategoria(request.categoriaId() == null ? null : buscarCategoria(request.categoriaId()));
    }

    private Categoria buscarCategoria(Integer categoriaId) {
        return categoriaRepository.findById(categoriaId)
                .orElseThrow(() -> new RequisicaoInvalidaException("CATEGORIA_INVALIDA", "Categoria não encontrada."));
    }

    private ConflitoException produtoDuplicado() {
        return new ConflitoException("PRODUTO_DUPLICADO", "Já existe um produto com este nome.");
    }
}
