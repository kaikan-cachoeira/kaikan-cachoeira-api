package com.kaikancachoeira.api.estoque.service;

import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.storage.ArmazenamentoService;
import com.kaikancachoeira.api.estoque.dto.FotoResponse;
import com.kaikancachoeira.api.estoque.dto.ProdutoRequest;
import com.kaikancachoeira.api.estoque.dto.ProdutoResponse;
import com.kaikancachoeira.api.estoque.entity.Categoria;
import com.kaikancachoeira.api.estoque.entity.Produto;
import com.kaikancachoeira.api.estoque.repository.CategoriaRepository;
import com.kaikancachoeira.api.estoque.repository.EstoqueRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public List<ProdutoResponse> listar(String q, List<Integer> categoriaIds, Boolean abaixoDoMinimo) {
        String texto = q == null ? "" : q.trim();

        return produtoRepository.buscarAtivos(texto).stream()
                .filter(p -> categoriaIds == null || categoriaIds.isEmpty()
                        || (p.getCategoria() != null && categoriaIds.contains(p.getCategoria().getId())))
                .filter(p -> !Boolean.TRUE.equals(abaixoDoMinimo)
                        || (p.getEstoqueMinimo() != null && p.getQuantidadeAtual() <= p.getEstoqueMinimo()))
                .map(ProdutoResponse::de)
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<ProdutoResponse> buscarPorId(Long id) {
        return produtoRepository.findByIdAndAtivoTrue(id).map(ProdutoResponse::de);
    }

    @Transactional
    public ProdutoResponse criar(ProdutoRequest request) {
        String nome = request.nome().trim();

        if (produtoRepository.existsByNomeIgnoreCase(nome)) {
            throw produtoDuplicado();
        }

        Produto produto = new Produto();
        preencher(produto, nome, request);

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