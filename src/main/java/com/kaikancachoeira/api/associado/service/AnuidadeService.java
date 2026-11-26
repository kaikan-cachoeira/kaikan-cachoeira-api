package com.kaikancachoeira.api.associado.service;

import com.kaikancachoeira.api.associado.dto.AnuidadeRequest;
import com.kaikancachoeira.api.associado.dto.AnuidadeResponse;
import com.kaikancachoeira.api.associado.dto.AnuidadeValorRequest;
import com.kaikancachoeira.api.associado.entity.Anuidade;
import com.kaikancachoeira.api.associado.entity.Associado;
import com.kaikancachoeira.api.associado.entity.ConfiguracaoAnuidade;
import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.enums.TipoAssociado;
import com.kaikancachoeira.api.associado.repository.AnuidadeRepository;
import com.kaikancachoeira.api.associado.repository.AssociadoRepository;
import com.kaikancachoeira.api.associado.repository.ConfiguracaoAnuidadeRepository;
import com.kaikancachoeira.api.associado.repository.PagamentoRepository;
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
    private final PagamentoRepository pagamentoRepository;

    public AnuidadeService(AnuidadeRepository anuidadeRepository,
                           AssociadoRepository associadoRepository,
                           ConfiguracaoAnuidadeRepository configuracaoRepository,
                           PagamentoRepository pagamentoRepository) {
        this.anuidadeRepository = anuidadeRepository;
        this.associadoRepository = associadoRepository;
        this.configuracaoRepository = configuracaoRepository;
        this.pagamentoRepository = pagamentoRepository;
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

    @Transactional
    public AnuidadeResponse isentar(Long id) {
        Anuidade anuidade = buscarAnuidade(id);

        if (anuidade.getStatus() == StatusAnuidade.ISENTO) {
            throw new ConflitoException("ANUIDADE_JA_ISENTA", "A anuidade já está isenta.");
        }

        anuidade.setStatus(StatusAnuidade.ISENTO);
        anuidadeRepository.save(anuidade);
        anuidadeRepository.flush();
        return AnuidadeResponse.de(anuidade);
    }

    @Transactional
    public void revogarIsencao(Long id) {
        Anuidade anuidade = buscarAnuidade(id);

        if (anuidade.getStatus() != StatusAnuidade.ISENTO) {
            throw new ConflitoException("ANUIDADE_NAO_ISENTA", "A anuidade não está isenta.");
        }

        anuidade.setStatus(statusComPagamentos(anuidade));
        anuidadeRepository.save(anuidade);
        anuidadeRepository.flush();
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

    private StatusAnuidade statusComPagamentos(Anuidade anuidade) {
        BigDecimal totalPago = pagamentoRepository.findAllByAnuidadeIdOrderByCriadoEmDesc(anuidade.getId()).stream()
                .map(pagamento -> pagamento.getValor())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return totalPago.compareTo(anuidade.getValor()) >= 0 ? StatusAnuidade.PAGO : StatusAnuidade.PENDENTE;
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
