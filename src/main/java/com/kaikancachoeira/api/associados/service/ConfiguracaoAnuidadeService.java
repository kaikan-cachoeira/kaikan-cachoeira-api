package com.kaikancachoeira.api.associados.service;

import com.kaikancachoeira.api.associados.dto.ConfiguracaoAnuidadeRequest;
import com.kaikancachoeira.api.associados.dto.ConfiguracaoAnuidadeResponse;
import com.kaikancachoeira.api.associados.entity.ConfiguracaoAnuidade;
import com.kaikancachoeira.api.associados.repository.ConfiguracaoAnuidadeRepository;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfiguracaoAnuidadeService {

    private final ConfiguracaoAnuidadeRepository repository;

    public ConfiguracaoAnuidadeService(ConfiguracaoAnuidadeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<ConfiguracaoAnuidadeResponse> listar() {
        return repository.findAllByOrderByAnoAsc().stream()
                .map(ConfiguracaoAnuidadeResponse::de)
                .toList();
    }

    @Transactional
    public ConfiguracaoAnuidadeResponse criar(ConfiguracaoAnuidadeRequest request) {
        validarDuplicidadeAno(request.ano(), null);
        ConfiguracaoAnuidade configuracao = new ConfiguracaoAnuidade();
        configuracao.setAno(request.ano());
        configuracao.setValor(request.valor());
        repository.save(configuracao);
        return ConfiguracaoAnuidadeResponse.de(configuracao);
    }

    @Transactional
    public ConfiguracaoAnuidadeResponse atualizar(Integer id, ConfiguracaoAnuidadeRequest request) {
        ConfiguracaoAnuidade configuracao = buscar(id);
        validarDuplicidadeAno(request.ano(), id);
        configuracao.setAno(request.ano());
        configuracao.setValor(request.valor());
        repository.save(configuracao);
        return ConfiguracaoAnuidadeResponse.de(configuracao);
    }

    @Transactional
    public void excluir(Integer id) {
        ConfiguracaoAnuidade configuracao = buscar(id);
        repository.delete(configuracao);
        repository.flush();
    }

    @Transactional(readOnly = true)
    public ConfiguracaoAnuidade buscar(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Configuração de anuidade", id));
    }

    private void validarDuplicidadeAno(Integer ano, Integer id) {
        boolean duplicado = (id == null)
                ? repository.existsByAno(ano)
                : repository.existsByAnoAndIdNot(ano, id);
        if (duplicado) {
            throw new ConflitoException("ANUIDADE_DUPLICADA", "Já existe uma configuração de anuidade para este ano.");
        }
    }
}
