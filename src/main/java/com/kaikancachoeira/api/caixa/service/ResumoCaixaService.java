package com.kaikancachoeira.api.caixa.service;

import com.kaikancachoeira.api.caixa.dto.ResumoCaixaResponse;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.repository.FiadoRepository;
import com.kaikancachoeira.api.caixa.repository.PagamentoCaixaRepository;
import com.kaikancachoeira.api.caixa.repository.TotalPorTipoForma;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class ResumoCaixaService {

    private final PagamentoCaixaRepository pagamentoRepository;
    private final FiadoRepository fiadoRepository;

    public ResumoCaixaService(PagamentoCaixaRepository pagamentoRepository, FiadoRepository fiadoRepository) {
        this.pagamentoRepository = pagamentoRepository;
        this.fiadoRepository = fiadoRepository;
    }

    @Transactional(readOnly = true)
    public ResumoCaixaResponse calcular(Long eventoId) {
        Map<FormaPagamento, BigDecimal> porForma = new LinkedHashMap<>();
        porForma.put(FormaPagamento.PIX, BigDecimal.ZERO);
        porForma.put(FormaPagamento.DEBITO, BigDecimal.ZERO);
        porForma.put(FormaPagamento.CREDITO, BigDecimal.ZERO);
        porForma.put(FormaPagamento.DINHEIRO, BigDecimal.ZERO);

        BigDecimal vendidas = BigDecimal.ZERO;
        BigDecimal devolvidas = BigDecimal.ZERO;

        for (TotalPorTipoForma linha : pagamentoRepository.totaisPorTipoEForma(eventoId)) {
            switch (linha.tipo()) {
                case VENDA_FICHA -> {
                    vendidas = vendidas.add(linha.total());
                    if (linha.formaPagamento() != FormaPagamento.FIADO) {
                        porForma.merge(linha.formaPagamento(), linha.total(), BigDecimal::add);
                    }
                }
                case DEVOLUCAO_FICHA -> {
                    devolvidas = devolvidas.add(linha.total());
                    porForma.merge(linha.formaPagamento(), linha.total().negate(), BigDecimal::add);
                }
                case QUITACAO_FIADO -> porForma.merge(linha.formaPagamento(), linha.total(), BigDecimal::add);
            }
        }

        BigDecimal totalRecebido = porForma.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal emAberto = fiadoRepository.somarEmAberto(eventoId);

        return new ResumoCaixaResponse(
                vendidas,
                devolvidas,
                vendidas.subtract(devolvidas),
                porForma,
                emAberto == null ? BigDecimal.ZERO : emAberto,
                fiadoRepository.contarEmAberto(eventoId),
                totalRecebido);
    }
}
