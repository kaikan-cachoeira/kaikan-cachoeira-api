package com.kaikancachoeira.api.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AlteracaoStatusEventoRequest(
        @Schema(description = "ID do novo status (1 NAO_INICIADO, 2 EM_ANDAMENTO, 3 ENCERRADO)", example = "3")
        @NotNull
        Integer status,

        @Schema(description = "Custos adicionais (limpeza, ajudantes etc.). Só aceitos na transição para ENCERRADO")
        List<@Valid CustoEventoRequest> custos
) {}
