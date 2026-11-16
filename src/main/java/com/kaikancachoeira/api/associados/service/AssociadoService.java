package com.kaikancachoeira.api.associados.service;

import com.kaikancachoeira.api.associados.dto.AssociadoListaResponse;
import com.kaikancachoeira.api.associados.dto.AssociadoRequest;
import com.kaikancachoeira.api.associados.dto.AssociadoResponse;
import com.kaikancachoeira.api.associados.entity.Associado;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import com.kaikancachoeira.api.associados.repository.AssociadoRepository;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class AssociadoService {

    private final AssociadoRepository repository;

    public AssociadoService(AssociadoRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public PaginaResponse<AssociadoListaResponse> listar(
            String q,
            LocalDate dataInicio,
            LocalDate dataFim,
            TipoAssociado tipo,
            Integer ano,
            StatusAnuidade status,
            int page,
            int size
    ) {
        return PaginaResponse.de(repository.buscar(
                q,
                dataInicio,
                dataFim,
                tipo,
                ano,
                status,
                PageRequest.of(page, size)
        ));
    }

    @Transactional
    public AssociadoResponse criar(AssociadoRequest request) {
        validarDuplicidadeNome(request.nome(), null);

        Associado associado = new Associado();
        aplicar(associado, request);
        repository.save(associado);
        return AssociadoResponse.de(associado);
    }

    @Transactional
    public AssociadoResponse atualizar(Long id, AssociadoRequest request) {
        Associado associado = buscar(id);
        validarDuplicidadeNome(request.nome(), id);
        aplicar(associado, request);
        repository.save(associado);
        return AssociadoResponse.de(associado);
    }

    @Transactional
    public void excluir(Long id) {
        Associado associado = buscar(id);
        repository.delete(associado);
        repository.flush();
    }

    @Transactional(readOnly = true)
    public Associado buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Associado", id));
    }

    @Transactional(readOnly = true)
    public Associado buscarTitular(Long id) {
        Associado associado = buscar(id);
        if (associado.getTipo() != TipoAssociado.TITULAR) {
            throw ValidacaoException.campo("titularId", "O associado informado não é um titular.");
        }
        return associado;
    }

    private void aplicar(Associado associado, AssociadoRequest request) {
        if (request.tipo() == TipoAssociado.TITULAR && request.titularId() != null) {
            throw ValidacaoException.campo("titularId", "Titular não deve informar titular responsável.");
        }

        if (request.tipo() == TipoAssociado.DEPENDENTE && request.titularId() == null) {
            throw ValidacaoException.campo("titularId", "Dependente deve informar o titular responsável.");
        }

        associado.setNome(request.nome().trim());
        associado.setNumeroContato(request.numeroContato());
        associado.setDataNascimento(request.dataNascimento());
        associado.setTipo(request.tipo());
        associado.setTitular((request.titularId() == null) ? null : buscarTitular(request.titularId()));
    }

    private void validarDuplicidadeNome(String nome, Long id) {
        boolean duplicado = (id == null)
                ? repository.existsByNomeIgnoreCase(nome.trim())
                : repository.existsByNomeIgnoreCaseAndIdNot(nome.trim(), id);
        if (duplicado) {
            throw new ConflitoException("ASSOCIADO_DUPLICADO", "Já existe um associado com este nome.");
        }
    }
}
