package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record VendaHora(
        @Schema(description = "Hora do dia, de 0 a 23", example = "14") int hora,
        long quantidadeVendas
) {}
