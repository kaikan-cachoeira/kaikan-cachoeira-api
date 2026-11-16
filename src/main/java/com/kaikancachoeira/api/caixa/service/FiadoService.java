package com.kaikancachoeira.api.caixa.service;

import com.kaikancachoeira.api.caixa.dto.FiadoListagemResponse;
import com.kaikancachoeira.api.caixa.dto.FiadoResponse;
import com.kaikancachoeira.api.caixa.dto.LancamentoResponse;
import com.kaikancachoeira.api.caixa.dto.QuitacaoRequest;
import com.kaikancachoeira.api.caixa.dto.QuitacaoResponse;
import com.kaikancachoeira.api.caixa.entity.Fiado;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import com.kaikancachoeira.api.caixa.repository.FiadoRepository;
import com.kaikancachoeira.api.caixa.repository.FiadoSpecifications;
import com.kaikancachoeira.api.caixa.repository.PagamentoCaixaRepository;
import com.kaikancachoeira.api.common.exception.CampoErro;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Service
public class FiadoService {

    private final EventoGuard eventoGuard;
    private final FiadoRepository fiadoRepository;
    private final PagamentoCaixaRepository pagamentoRepository;
    private final ResumoCaixaService resumoService;
    private final Clock clock;

    public FiadoService(EventoGuard eventoGuard,
                        FiadoRepository fiadoRepository,
                        PagamentoCaixaRepository pagamentoRepository,
                        ResumoCaixaService resumoService,
                        Clock clock) {
        this.eventoGuard = eventoGuard;
        this.fiadoRepository = fiadoRepository;
        this.pagamentoRepository = pagamentoRepository;
        this.resumoService = resumoService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FiadoListagemResponse listar(Long eventoId, String situacao, int page, int size) {
        eventoGuard.buscar(eventoId);
        Boolean quitado = converterSituacao(situacao);

        Specification<Fiado> spec = Specification.where(FiadoSpecifications.doEvento(eventoId))
                .and(FiadoSpecifications.quitado(quitado));

        Page<Fiado> pagina = fiadoRepository.findAll(
                spec,
                PageRequest.of(page, size, Sort.by(Sort.Order.desc("criadoEm"), Sort.Order.desc("id")))
        );

        long pendingCount = calcularPendingCount(eventoId, quitado);
        BigDecimal openAmount = calcularOpenAmount(eventoId, quitado);

        return new FiadoListagemResponse(
                pagina.map(FiadoResponse::de).getContent(),
                pagina.getNumber(),
                pagina.getSize(),
                pagina.getTotalElements(),
                pagina.getTotalPages(),
                pendingCount,
                openAmount
        );
    }

    @Transactional
    public QuitacaoResponse quitar(Long eventoId, Long fiadoId, QuitacaoRequest request) {
        Evento evento = eventoGuard.buscar(eventoId);

        if (request.formaPagamento() == FormaPagamento.FIADO) {
            throw ValidacaoException.campo("formaPagamento", "Forma de pagamento da quitação não pode ser FIADO");
        }

        Fiado fiado = fiadoRepository.buscarParaAtualizacao(fiadoId)
                .filter(f -> f.getLancamentoOrigem().getEvento().getId().equals(eventoId))
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Fiado", fiadoId));

        if (fiado.getQuitado()) {
            throw new ConflitoException("FIADO_JA_QUITADO", "O fiado já está quitado.");
        }

        fiado.quitar(request.formaPagamento(), Instant.now(clock));

        PagamentoCaixa lancamento = new PagamentoCaixa();
        lancamento.setEvento(evento);
        lancamento.setTipo(TipoLancamento.QUITACAO_FIADO);
        lancamento.setValor(fiado.getValor());
        lancamento.setFormaPagamento(request.formaPagamento());
        lancamento.setFiadoQuitado(fiado);
        pagamentoRepository.save(lancamento);

        return new QuitacaoResponse(FiadoResponse.de(fiado),
                LancamentoResponse.de(lancamento, fiado.getNomeDevedor()),
                resumoService.calcular(eventoId));
    }

    private Boolean converterSituacao(String situacao) {
        if (situacao == null || situacao.isBlank()) {
            return null;
        }
        return switch (situacao.trim().toLowerCase()) {
            case "pendente" -> false;
            case "quitado" -> true;
            default -> throw new RequisicaoInvalidaException(
                    "REQUISICAO_INVALIDA",
                    "Valor inválido para o parâmetro",
                    List.of(new CampoErro("situacao", "Valor inválido"))
            );
        };
    }

    private long calcularPendingCount(Long eventoId, Boolean quitado) {
        if (Boolean.TRUE.equals(quitado)) {
            return 0L;
        }
        return fiadoRepository.contarEmAberto(eventoId);
    }

    private BigDecimal calcularOpenAmount(Long eventoId, Boolean quitado) {
        if (Boolean.TRUE.equals(quitado)) {
            return BigDecimal.ZERO;
        }
        BigDecimal soma = fiadoRepository.somarEmAberto(eventoId);
        return soma == null ? BigDecimal.ZERO : soma;
    }
}
