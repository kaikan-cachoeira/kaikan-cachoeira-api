package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimentacaoResponse(
        Long id,
        Long estoqueId,
        String tipo,
        Integer quantidade,
        BigDecimal custoTotal,
        BigDecimal custoUnitario,
        Long eventoId,
        String observacoes,
        Instant criadoEm
) {

    public static MovimentacaoResponse de(MovimentacaoEstoque m) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getEstoque().getId(),
                m.getTipo().getCodigo(),
                m.getQuantidade(),
                m.getCustoTotal(),
                m.getCustoUnitario(),
                m.getEventoId(),
                m.getObservacoes(),
                m.getCriadoEm());
    }
}