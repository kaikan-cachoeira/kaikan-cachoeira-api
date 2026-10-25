package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;

import java.math.BigDecimal;

public record DoacaoDinheiroRelatorio(
        String doador,
        BigDecimal valor,
        FormaPagamentoDoacao formaPagamento,
        BigDecimal valorFicha
) {
}
