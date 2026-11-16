package com.kaikancachoeira.api.associados.dto;

import com.kaikancachoeira.api.associados.entity.Pagamento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Representação de um pagamento de anuidade.")
public record PagamentoResponse(
        Long id,
        Long anuidadeId,
        Integer ano,
        BigDecimal valor
) {
    public static PagamentoResponse de(Pagamento pagamento) {
        return new PagamentoResponse(
                pagamento.getId(),
                pagamento.getAnuidade() == null ? null : pagamento.getAnuidade().getId(),
                pagamento.getAnuidade() == null || pagamento.getAnuidade().getConfiguracaoAnuidade() == null
                        ? null
                        : pagamento.getAnuidade().getConfiguracaoAnuidade().getAno(),
                pagamento.getValor()
        );
    }
}
