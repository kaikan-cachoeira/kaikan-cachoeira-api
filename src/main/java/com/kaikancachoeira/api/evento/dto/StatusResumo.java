package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.StatusEvento;

public record StatusResumo(
        Integer id,
        String codigo,
        String descricao
) {

    public static StatusResumo de(StatusEvento s) {
        return new StatusResumo(s.getId(), s.getCodigo(), s.getDescricao());
    }

}