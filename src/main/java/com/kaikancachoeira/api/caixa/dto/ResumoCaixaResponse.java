package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;

import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "Resumo financeiro dos lançamentos de caixa de um evento.")
public record ResumoCaixaResponse(
        @Schema(description = "Valor total recebido pela venda de fichas.")
        BigDecimal fichasVendidas,
        @Schema(description = "Valor total devolvido pela devolução de fichas.")
        BigDecimal fichasDevolvidas,
        @Schema(description = "Saldo de fichas, calculado pelas vendas menos as devoluções.")
        BigDecimal saldoFichas,
        @Schema(description = "Totais agrupados por forma de pagamento.")
        Map<FormaPagamento, BigDecimal> porFormaPagamento,
        @Schema(description = "Valor total dos fiados ainda não quitados.")
        BigDecimal fiadosEmAberto,
        @Schema(description = "Quantidade de fiados ainda não quitados.")
        long quantidadeFiadosEmAberto,
        @Schema(description = "Valor total recebido no caixa.")
        BigDecimal totalRecebido
) {}