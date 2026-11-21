package com.kaikancachoeira.api.associado.service;

import com.kaikancachoeira.api.associado.dto.ConfiguracaoAnuidadeRequest;
import com.kaikancachoeira.api.associado.dto.ConfiguracaoAnuidadeResponse;
import com.kaikancachoeira.api.associado.entity.ConfiguracaoAnuidade;
import com.kaikancachoeira.api.associado.repository.ConfiguracaoAnuidadeRepository;
import com.kaikancachoeira.api.associado.repository.PagamentoRepository;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ConfiguracaoAnuidadeService {

    private final ConfiguracaoAnuidadeRepository repository;
    private final PagamentoRepository pagamentoRepository;

    public ConfiguracaoAnuidadeService(ConfiguracaoAnuidadeRepository repository, PagamentoRepository pagamentoRepository) {
        this.repository = repository;
        this.pagamentoRepository = pagamentoRepository;
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

        if (pagamentoRepository.existsByAnuidade_ConfiguracaoAnuidadeId(id)) {
            throw new ConflitoException(
                    "CONFIGURACAO_ANUIDADE_COM_PAGAMENTOS",
                    "Não é possível excluir esta configuração porque existem pagamentos vinculados às anuidades geradas por ela."
            );
        }

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
