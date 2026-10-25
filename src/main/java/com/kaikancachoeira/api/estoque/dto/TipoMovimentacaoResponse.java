package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Direcao;
import com.kaikancachoeira.api.estoque.entity.TipoMovimentacao;
import io.swagger.v3.oas.annotations.media.Schema;

public record TipoMovimentacaoResponse(
        Integer id,
        @Schema(example = "REABASTECIMENTO") String codigo,
        String descricao,
        Direcao direcao,
        @Schema(description = "Se true, a entrada exige custoTotal") Boolean exigeCusto
) {
    public static TipoMovimentacaoResponse de(TipoMovimentacao t) {
        return new TipoMovimentacaoResponse(t.getId(), t.getCodigo(), t.getDescricao(), t.getDirecao(), t.getExigeCusto());
    }
}
