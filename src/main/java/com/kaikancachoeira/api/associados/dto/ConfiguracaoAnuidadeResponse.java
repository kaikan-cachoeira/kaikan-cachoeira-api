package com.kaikancachoeira.api.associados.dto;

import com.kaikancachoeira.api.associados.entity.ConfiguracaoAnuidade;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Representação de uma configuração de anuidade.")
public record ConfiguracaoAnuidadeResponse(
        Integer id,
        Integer ano,
        BigDecimal valor
) {
    public static ConfiguracaoAnuidadeResponse de(ConfiguracaoAnuidade configuracao) {
        return new ConfiguracaoAnuidadeResponse(
                configuracao.getId(),
                configuracao.getAno(),
                configuracao.getValor()
        );
    }
}
