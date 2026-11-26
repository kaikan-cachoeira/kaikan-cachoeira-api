package com.kaikancachoeira.api.clima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record ClimaResponse(
        ClimaResumoResponse resumo,
        @Schema(description = "Leituras climáticas em ordem crescente de data e hora") List<ClimaItemResponse> itens
) {}
