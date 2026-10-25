package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.doacao.enums.FormaPagamentoDoacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record DoacaoDinheiroRelatorio(
        String doador,
        BigDecimal valor,
        FormaPagamentoDoacao formaPagamento,
        @Schema(nullable = true) BigDecimal valorFicha
) {}