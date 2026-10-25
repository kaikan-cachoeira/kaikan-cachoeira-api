package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.doacao.enums.TipoDoacao;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record DoacaoItemRelatorio(
        TipoDoacao tipo,
        @Schema(nullable = true) Long produtoId,
        String nome,
        @Schema(nullable = true) Integer quantidade,
        String doador,
        @Schema(nullable = true) BigDecimal valorFicha
) {}