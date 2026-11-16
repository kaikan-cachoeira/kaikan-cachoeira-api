package com.kaikancachoeira.api.associados.dto;

import com.kaikancachoeira.api.associados.entity.Anuidade;
import com.kaikancachoeira.api.associados.enums.StatusAnuidade;
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
