package com.kaikancachoeira.api.caixa.service;

import com.kaikancachoeira.api.caixa.dto.FiadoResponse;
import com.kaikancachoeira.api.caixa.dto.LancamentoResponse;
import com.kaikancachoeira.api.caixa.dto.QuitacaoRequest;
import com.kaikancachoeira.api.caixa.dto.QuitacaoResponse;
import com.kaikancachoeira.api.caixa.entity.Fiado;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import com.kaikancachoeira.api.caixa.repository.FiadoRepository;
import com.kaikancachoeira.api.caixa.repository.PagamentoCaixaRepository;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    public List<FiadoResponse> listar(Long eventoId, String situacao) {
        eventoGuard.buscar(eventoId);
        Boolean quitado = converterSituacao(situacao);
        return fiadoRepository.listar(eventoId, quitado).stream().map(FiadoResponse::de).toList();
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
            default -> throw ValidacaoException.campo("situacao", "Situação deve ser pendente ou quitado");
        };
    }
}
