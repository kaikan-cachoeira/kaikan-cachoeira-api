package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.Fiado;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;

import java.math.BigDecimal;
import java.time.Instant;

public record FiadoResponse(
        Long id,
        String nomeDevedor,
        BigDecimal valor,
        String observacoes,
        boolean quitado,
        Instant quitadoEm,
        FormaPagamento formaPagamento,
        Instant criadoEm
) {
    public static FiadoResponse de(Fiado fiado) {
        return new FiadoResponse(fiado.getId(), fiado.getNomeDevedor(), fiado.getValor(), fiado.getObservacoes(),
                fiado.getQuitado(), fiado.getQuitadoEm(), fiado.getFormaPagamento(), fiado.getCriadoEm());
    }
}
