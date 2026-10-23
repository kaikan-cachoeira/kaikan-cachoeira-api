package com.kaikancachoeira.api.estoque.service;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.estoque.dto.CategoriaRequest;
import com.kaikancachoeira.api.estoque.dto.CategoriaResponse;
import com.kaikancachoeira.api.estoque.entity.Categoria;
import com.kaikancachoeira.api.estoque.repository.CategoriaRepository;
import com.kaikancachoeira.api.estoque.repository.ProdutoRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final ProdutoRepository produtoRepository;

    public CategoriaService(CategoriaRepository categoriaRepository, ProdutoRepository produtoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.produtoRepository = produtoRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNomeAsc().stream()
                .map(CategoriaResponse::de)
                .toList();
    }

    @Transactional
    public CategoriaResponse criar(CategoriaRequest request) {
        String nome = request.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCase(nome)) {
            throw new ConflitoException("CATEGORIA_DUPLICADA", "Já existe uma categoria com este nome.");
        }

        Categoria categoria = new Categoria();
        categoria.setNome(nome);
        categoria.setDescricao(request.descricao());

        return CategoriaResponse.de(categoriaRepository.save(categoria));
    }

    @Transactional
    public CategoriaResponse atualizar(Integer id, CategoriaRequest request) {
        Categoria categoria = buscar(id);
        String nome = request.nome().trim();

        if (categoriaRepository.existsByNomeIgnoreCaseAndIdNot(nome, id)) {
            throw new ConflitoException("CATEGORIA_DUPLICADA", "Já existe uma categoria com este nome.");
        }

        categoria.setNome(nome);
        categoria.setDescricao(request.descricao());

        return CategoriaResponse.de(categoria);
    }

    @Transactional
    public void excluir(Integer id) {
        Categoria categoria = buscar(id);

        if (produtoRepository.existsByCategoriaId(id)) {
            throw categoriaComProdutos();
        }

        try {
            categoriaRepository.delete(categoria);
            categoriaRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw categoriaComProdutos();
        }
    }

    private Categoria buscar(Integer id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Categoria", id));
    }

    private ConflitoException categoriaComProdutos() {
        return new ConflitoException("CATEGORIA_COM_PRODUTOS",
                "Não é possível excluir esta categoria porque existem produtos vinculados a ela.");
    }
}