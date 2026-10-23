package com.kaikancachoeira.api.estoque.dto;

import com.kaikancachoeira.api.estoque.entity.Direcao;
import com.kaikancachoeira.api.estoque.entity.TipoMovimentacao;

public record TipoMovimentacaoResponse(Integer id, String codigo, String descricao, Direcao direcao, Boolean exigeCusto) {

    public static TipoMovimentacaoResponse de(TipoMovimentacao t) {
        return new TipoMovimentacaoResponse(t.getId(), t.getCodigo(), t.getDescricao(), t.getDirecao(), t.getExigeCusto());
    }
}
