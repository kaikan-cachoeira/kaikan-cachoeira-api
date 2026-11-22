package com.kaikancachoeira.api.associado.dto;

import com.kaikancachoeira.api.associado.entity.Anuidade;
import com.kaikancachoeira.api.associado.enums.StatusAnuidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Representação de uma anuidade.")
public record AnuidadeResponse(
        Long id,
        Long associadoId,
        Integer idConfAnuidade,
        Integer ano,
        BigDecimal valor,
        StatusAnuidade status
) {
    public static AnuidadeResponse de(Anuidade anuidade) {
        return new AnuidadeResponse(
                anuidade.getId(),
                anuidade.getAssociado() == null ? null : anuidade.getAssociado().getId(),
                anuidade.getConfiguracaoAnuidade() == null ? null : anuidade.getConfiguracaoAnuidade().getId(),
                anuidade.getConfiguracaoAnuidade() == null ? null : anuidade.getConfiguracaoAnuidade().getAno(),
                anuidade.getValor(),
                anuidade.getStatus()
        );
    }
}
