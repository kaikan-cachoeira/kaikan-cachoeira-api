package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;

import java.math.BigDecimal;
import java.util.Map;

public record ResumoCaixaResponse(
        BigDecimal fichasVendidas,
        BigDecimal fichasDevolvidas,
        BigDecimal saldoFichas,
        Map<FormaPagamento, BigDecimal> porFormaPagamento,
        BigDecimal fiadosEmAberto,
        long quantidadeFiadosEmAberto,
        BigDecimal totalRecebido
) {}
