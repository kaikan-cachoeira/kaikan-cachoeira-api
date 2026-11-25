package com.kaikancachoeira.api.associado.service;

import com.kaikancachoeira.api.associado.dto.PagamentoRequest;
import com.kaikancachoeira.api.associado.dto.PagamentoResponse;
import com.kaikancachoeira.api.associado.entity.Anuidade;
import com.kaikancachoeira.api.associado.entity.Pagamento;
import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import com.kaikancachoeira.api.associado.repository.AnuidadeRepository;
import com.kaikancachoeira.api.associado.repository.PagamentoRepository;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class PagamentoService {

    private final PagamentoRepository pagamentoRepository;
    private final AnuidadeRepository anuidadeRepository;

    public PagamentoService(PagamentoRepository pagamentoRepository, AnuidadeRepository anuidadeRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.anuidadeRepository = anuidadeRepository;
    }

    @Transactional
    public PagamentoResponse registrar(Long idAssociado, PagamentoRequest request) {
        Anuidade anuidade = anuidadeRepository.findByAssociadoIdAndConfiguracaoAnuidadeId(idAssociado, request.confAnuidadeId())
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Anuidade", request.confAnuidadeId()));

        if (anuidade.getStatus() == StatusAnuidade.ISENTO) {
            throw new ConflitoException("ANUIDADE_ISENTA", "Não é possível registrar pagamento em anuidade isenta.");
        }

        Pagamento pagamento = new Pagamento();
        pagamento.setAnuidade(anuidade);
        pagamento.setValor(request.valor());
        pagamentoRepository.save(pagamento);

        atualizarStatus(anuidade);

        return PagamentoResponse.de(pagamento);
    }

    @Transactional
    public void excluir(Long idAssociado, Long idPagamento) {
        Pagamento pagamento = pagamentoRepository.findById(idPagamento)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Pagamento", idPagamento));

        if (pagamento.getAnuidade() == null || pagamento.getAnuidade().getAssociado() == null
                || !pagamento.getAnuidade().getAssociado().getId().equals(idAssociado)) {
            throw RecursoNaoEncontradoException.de("Pagamento", idPagamento);
        }

        Anuidade anuidade = pagamento.getAnuidade();
        pagamentoRepository.delete(pagamento);
        pagamentoRepository.flush();

        atualizarStatus(anuidade);
    }

    private void atualizarStatus(Anuidade anuidade) {
        if (anuidade.getStatus() == StatusAnuidade.ISENTO) {
            return;
        }

        BigDecimal totalPago = pagamentoRepository.findAllByAnuidadeIdOrderByCriadoEmDesc(anuidade.getId()).stream()
                .map(Pagamento::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        anuidade.setStatus(totalPago.compareTo(anuidade.getValor()) >= 0 ? StatusAnuidade.PAGO : StatusAnuidade.PENDENTE);
    }
}
