package com.kaikancachoeira.api.associados.service;

import com.kaikancachoeira.api.associados.dto.AnuidadeRequest;
import com.kaikancachoeira.api.associados.dto.AnuidadeResponse;
import com.kaikancachoeira.api.associados.dto.AnuidadeValorRequest;
import com.kaikancachoeira.api.associados.entity.Anuidade;
import com.kaikancachoeira.api.associados.entity.Associado;
import com.kaikancachoeira.api.associados.entity.ConfiguracaoAnuidade;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
import com.kaikancachoeira.api.associados.enums.TipoAssociado;
import com.kaikancachoeira.api.associados.repository.AnuidadeRepository;
import com.kaikancachoeira.api.associados.repository.AssociadoRepository;
import com.kaikancachoeira.api.associados.repository.ConfiguracaoAnuidadeRepository;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class AnuidadeService {

    private final AnuidadeRepository anuidadeRepository;
    private final AssociadoRepository associadoRepository;
    private final ConfiguracaoAnuidadeRepository configuracaoRepository;

    public AnuidadeService(AnuidadeRepository anuidadeRepository,
                           AssociadoRepository associadoRepository,
                           ConfiguracaoAnuidadeRepository configuracaoRepository) {
        this.anuidadeRepository = anuidadeRepository;
        this.associadoRepository = associadoRepository;
        this.configuracaoRepository = configuracaoRepository;
    }

    @Transactional
    public void criarParaTodosTitulares(AnuidadeRequest request) {
        ConfiguracaoAnuidade configuracao = buscarConfiguracao(request.idConfAnuidade());

        associadoRepository.findAllByTipoOrderByNomeAsc(TipoAssociado.TITULAR).forEach(associado ->
                criarSeNaoExistir(associado, configuracao, request.valor())
        );
    }

    @Transactional
    public void criarParaAssociado(Long idAssociado, AnuidadeRequest request) {
        Associado associado = buscarAssociado(idAssociado);
        ConfiguracaoAnuidade configuracao = buscarConfiguracao(request.idConfAnuidade());
        criarSeNaoExistir(associado, configuracao, request.valor());
    }

    @Transactional
    public AnuidadeResponse atualizarValor(Long id, AnuidadeValorRequest request) {
        Anuidade anuidade = buscarAnuidade(id);
        anuidade.setValor(request.valor());
        anuidadeRepository.save(anuidade);
        anuidadeRepository.flush();
        return AnuidadeResponse.de(anuidade);
    }

    @Transactional(readOnly = true)
    public Anuidade buscarAnuidade(Long id) {
        return anuidadeRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Anuidade", id));
    }

    private void criarSeNaoExistir(Associado associado, ConfiguracaoAnuidade configuracao, BigDecimal valor) {
        if (anuidadeRepository.findByAssociadoIdAndConfiguracaoAnuidadeId(associado.getId(), configuracao.getId()).isPresent()) {
            throw new ConflitoException("ANUIDADE_JA_EXISTE", "Já existe anuidade para este associado e esta configuração.");
        }

        Anuidade anuidade = new Anuidade();
        anuidade.setAssociado(associado);
        anuidade.setConfiguracaoAnuidade(configuracao);
        anuidade.setValor(valor);
        anuidade.setStatus(StatusAnuidade.PENDENTE);
        anuidadeRepository.save(anuidade);
    }

    private ConfiguracaoAnuidade buscarConfiguracao(Integer id) {
        return configuracaoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Configuração de anuidade", id));
    }

    private Associado buscarAssociado(Long id) {
        return associadoRepository.findById(id)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Associado", id));
    }
}
