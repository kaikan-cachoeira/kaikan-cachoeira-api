package com.kaikancachoeira.api.caixa.service;

import com.kaikancachoeira.api.caixa.dto.CaixaRequest;
import com.kaikancachoeira.api.caixa.dto.CaixaResponse;
import com.kaikancachoeira.api.caixa.dto.ExclusaoCaixaResponse;
import com.kaikancachoeira.api.caixa.dto.LancamentoResponse;
import com.kaikancachoeira.api.caixa.dto.RegistroCaixaResponse;
import com.kaikancachoeira.api.caixa.entity.Fiado;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import com.kaikancachoeira.api.caixa.repository.FiadoRepository;
import com.kaikancachoeira.api.caixa.repository.PagamentoCaixaRepository;
import com.kaikancachoeira.api.common.dto.PaginaResponse;
import com.kaikancachoeira.api.common.exception.ConflitoException;
import com.kaikancachoeira.api.common.exception.RecursoNaoEncontradoException;
import com.kaikancachoeira.api.common.exception.ValidacaoException;
import com.kaikancachoeira.api.evento.entity.Evento;
import com.kaikancachoeira.api.evento.service.EventoGuard;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CaixaService {

    private static final int TAMANHO_PADRAO = 20;
    private static final int TAMANHO_MAXIMO = 100;

    private final EventoGuard eventoGuard;
    private final PagamentoCaixaRepository pagamentoRepository;
    private final FiadoRepository fiadoRepository;
    private final ResumoCaixaService resumoService;

    public CaixaService(EventoGuard eventoGuard,
                        PagamentoCaixaRepository pagamentoRepository,
                        FiadoRepository fiadoRepository,
                        ResumoCaixaService resumoService) {
        this.eventoGuard = eventoGuard;
        this.pagamentoRepository = pagamentoRepository;
        this.fiadoRepository = fiadoRepository;
        this.resumoService = resumoService;
    }

    @Transactional
    public RegistroCaixaResponse registrar(Long eventoId, CaixaRequest request) {
        Evento evento = eventoGuard.exigirEmAndamento(eventoId);
        validar(request);

        PagamentoCaixa pagamento = new PagamentoCaixa();
        pagamento.setEvento(evento);
        pagamento.setTipo(request.tipo());
        pagamento.setValor(request.valor().setScale(2, RoundingMode.HALF_UP));
        pagamento.setFormaPagamento(request.formaPagamento());
        pagamentoRepository.save(pagamento);

        String nomeDevedor = null;
        if (request.formaPagamento() == FormaPagamento.FIADO) {
            Fiado fiado = new Fiado();
            fiado.setLancamentoOrigem(pagamento);
            fiado.setNomeDevedor(request.nomeDevedor().trim());
            fiado.setObservacoes(textoOuNulo(request.observacao()));
            fiado.setValor(pagamento.getValor());
            fiadoRepository.save(fiado);
            nomeDevedor = fiado.getNomeDevedor();
        }

        return new RegistroCaixaResponse(LancamentoResponse.de(pagamento, nomeDevedor),
                resumoService.calcular(eventoId));
    }

    @Transactional(readOnly = true)
    public CaixaResponse listar(Long eventoId, TipoLancamento tipo, FormaPagamento formaPagamento,
                                int page, int size) {
        eventoGuard.buscar(eventoId);

        int tamanho = size <= 0 ? TAMANHO_PADRAO : Math.min(size, TAMANHO_MAXIMO);
        Page<PagamentoCaixa> pagina = pagamentoRepository.buscar(eventoId, tipo, formaPagamento,
                PageRequest.of(Math.max(page, 0), tamanho));

        List<Long> origensFiado = pagina.getContent().stream()
                .filter(p -> p.getTipo() == TipoLancamento.VENDA_FICHA && p.getFormaPagamento() == FormaPagamento.FIADO)
                .map(PagamentoCaixa::getId)
                .toList();

        Map<Long, String> nomePorLancamento = new HashMap<>();
        if (!origensFiado.isEmpty()) {
            fiadoRepository.buscarPorLancamentosOrigem(origensFiado)
                    .forEach(f -> nomePorLancamento.put(f.getLancamentoOrigem().getId(), f.getNomeDevedor()));
        }

        Page<LancamentoResponse> lancamentos = pagina.map(p -> LancamentoResponse.de(p, nomeDevedor(p, nomePorLancamento)));
        return new CaixaResponse(resumoService.calcular(eventoId), PaginaResponse.de(lancamentos));
    }

    @Transactional
    public ExclusaoCaixaResponse excluir(Long eventoId, Long lancamentoId) {
        eventoGuard.exigirEmAndamento(eventoId);
        PagamentoCaixa pagamento = pagamentoRepository.findByIdAndEventoId(lancamentoId, eventoId)
                .orElseThrow(() -> RecursoNaoEncontradoException.de("Lançamento", lancamentoId));

        if (pagamento.getTipo() == TipoLancamento.QUITACAO_FIADO) {
            Fiado fiado = pagamento.getFiadoQuitado();
            fiado.reabrir();
            pagamentoRepository.delete(pagamento);
        } else if (pagamento.getFormaPagamento() == FormaPagamento.FIADO) {
            Fiado fiado = fiadoRepository.buscarPorLancamentoOrigem(pagamento.getId())
                    .orElseThrow(() -> new IllegalStateException("Fiado não encontrado para o lançamento " + lancamentoId));
            if (fiado.getQuitado()) {
                throw new ConflitoException("FIADO_JA_QUITADO",
                        "O fiado já foi quitado. Exclua primeiro o lançamento da quitação.");
            }
            fiadoRepository.delete(fiado);
            fiadoRepository.flush();
            pagamentoRepository.delete(pagamento);
        } else {
            pagamentoRepository.delete(pagamento);
        }
        pagamentoRepository.flush();

        return new ExclusaoCaixaResponse(resumoService.calcular(eventoId));
    }

    private void validar(CaixaRequest request) {
        if (request.tipo() == TipoLancamento.QUITACAO_FIADO) {
            throw ValidacaoException.campo("tipo", "Tipo deve ser VENDA_FICHA ou DEVOLUCAO_FICHA");
        }
        if (request.formaPagamento() == FormaPagamento.FIADO) {
            if (request.tipo() != TipoLancamento.VENDA_FICHA) {
                throw ValidacaoException.campo("formaPagamento", "Fiado só é aceito em venda de fichas");
            }
            if (request.nomeDevedor() == null || request.nomeDevedor().isBlank()) {
                throw ValidacaoException.campo("nomeDevedor", "Nome é obrigatório para fiado");
            }
        }
    }

    private String nomeDevedor(PagamentoCaixa pagamento, Map<Long, String> nomePorLancamento) {
        if (pagamento.getTipo() == TipoLancamento.QUITACAO_FIADO) {
            return pagamento.getFiadoQuitado().getNomeDevedor();
        }
        return nomePorLancamento.get(pagamento.getId());
    }

    private String textoOuNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
