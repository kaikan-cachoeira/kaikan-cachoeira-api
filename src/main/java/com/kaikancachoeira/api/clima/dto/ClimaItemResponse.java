package com.kaikancachoeira.api.clima.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;

public record ClimaItemResponse(
        @Schema(description = "Data e hora prevista da leitura", example = "2026-11-21T14:00:00Z") Instant dataHoraPrevista,
        @Schema(description = "Temperatura prevista") BigDecimal temperatura,
        @Schema(description = "Sensação térmica prevista") BigDecimal sensacaoTermica,
        @Schema(description = "Volume de chuva previsto em mm") BigDecimal chuvaMm,
        @Schema(description = "Probabilidade de chuva em percentual") Integer probabilidadeChuva,
        @Schema(description = "Velocidade do vento prevista") BigDecimal ventoVelocidade,
        @Schema(description = "Código numérico do clima retornado pela Open Meteo") Integer codigoClima,
        @Schema(description = "Descrição textual do clima") String descricaoClima
) {}
