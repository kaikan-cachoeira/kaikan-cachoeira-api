package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.StatusEvento;
import io.swagger.v3.oas.annotations.media.Schema;

public record StatusResumo(
        @Schema(description = "1 = NAO_INICIADO, 2 = EM_ANDAMENTO, 3 = ENCERRADO", example = "2") Integer id,
        @Schema(example = "EM_ANDAMENTO") String codigo,
        @Schema(example = "Evento em andamento") String descricao
) {

    public static StatusResumo de(StatusEvento s) {
        return new StatusResumo(s.getId(), s.getCodigo(), s.getDescricao());
    }

}