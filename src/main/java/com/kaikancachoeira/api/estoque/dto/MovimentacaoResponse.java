package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.MovimentacaoEstoque;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record MovimentacaoResponse(
        Long id,
        @Schema(description = "Lote afetado pela movimentação") Long estoqueId,
        @Schema(description = "Código do tipo", example = "REABASTECIMENTO") String tipo,
        @Schema(description = "Positiva em entradas, negativa em saídas", example = "-3") Integer quantidade,
        @Schema(description = "Custo do lote movimentado") BigDecimal custoTotal,
        BigDecimal custoUnitario,
        @Schema(nullable = true) Long eventoId,
        @Schema(nullable = true) String eventoNome,
        @Schema(nullable = true) String observacoes,
        Instant criadoEm
) {

    public static MovimentacaoResponse de(MovimentacaoEstoque m, String eventoNome) {
        return new MovimentacaoResponse(
                m.getId(),
                m.getEstoque().getId(),
                m.getTipo().getCodigo(),
                m.getQuantidade(),
                m.getCustoTotal(),
                m.getCustoUnitario(),
                m.getEventoId(),
                eventoNome,
                m.getObservacoes(),
                m.getCriadoEm());
    }
}
