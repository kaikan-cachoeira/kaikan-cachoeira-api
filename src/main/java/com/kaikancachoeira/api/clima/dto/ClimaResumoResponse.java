package com.kaikancachoeira.api.clima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ClimaResumoResponse(
        @Schema(description = "Média da temperatura no período") BigDecimal temperaturaMedia,
        @Schema(description = "Menor temperatura no período") BigDecimal temperaturaMinima,
        @Schema(description = "Maior temperatura no período") BigDecimal temperaturaMaxima,
        @Schema(description = "Soma da chuva prevista no período") BigDecimal chuvaAcumulada,
        @Schema(description = "Maior velocidade do vento no período") BigDecimal ventoMaximo
) {}
