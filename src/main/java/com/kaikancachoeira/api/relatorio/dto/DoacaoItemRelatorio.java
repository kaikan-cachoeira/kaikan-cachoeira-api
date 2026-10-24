package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.doacao.enums.TipoDoacao;

import java.math.BigDecimal;

public record DoacaoItemRelatorio(
        TipoDoacao tipo,
        Long produtoId,
        String nome,
        Integer quantidade,
        String doador,
        BigDecimal valorFicha
) {
}
