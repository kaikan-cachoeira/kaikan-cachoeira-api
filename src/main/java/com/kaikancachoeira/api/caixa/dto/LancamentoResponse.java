package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;

import java.math.BigDecimal;
import java.time.Instant;

public record LancamentoResponse(
        Long id,
        TipoLancamento tipo,
        BigDecimal valor,
        FormaPagamento formaPagamento,
        String nomeDevedor,
        Instant criadoEm
) {
    public static LancamentoResponse de(PagamentoCaixa pagamento, String nomeDevedor) {
        return new LancamentoResponse(pagamento.getId(), pagamento.getTipo(), pagamento.getValor(),
                pagamento.getFormaPagamento(), nomeDevedor, pagamento.getCriadoEm());
    }
}
