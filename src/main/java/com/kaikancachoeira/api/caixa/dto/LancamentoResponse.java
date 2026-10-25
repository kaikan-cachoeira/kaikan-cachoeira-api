package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.PagamentoCaixa;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Representação de um lançamento de caixa.")
public record LancamentoResponse(
        @Schema(description = "Identificador do lançamento.")
        Long id,
        @Schema(description = "Tipo do lançamento, como venda ou devolução de fichas.")
        TipoLancamento tipo,
        @Schema(description = "Valor do lançamento em reais.")
        BigDecimal valor,
        @Schema(description = "Forma de pagamento utilizada.")
        FormaPagamento formaPagamento,
        @Schema(description = "Nome do devedor quando o lançamento corresponde a uma venda fiada; caso contrário, pode ser nulo.")
        String nomeDevedor,
        @Schema(description = "Data e hora de criação do lançamento em ISO-8601.")
        Instant criadoEm
) {
    public static LancamentoResponse de(PagamentoCaixa pagamento, String nomeDevedor) {
        return new LancamentoResponse(pagamento.getId(), pagamento.getTipo(), pagamento.getValor(),
                pagamento.getFormaPagamento(), nomeDevedor, pagamento.getCriadoEm());
    }
}