package com.kaikancachoeira.api.clima.client;

import java.math.BigDecimal;
import java.time.Instant;

public record ClimaPrevisaoHoraria(
        Instant dataHoraPrevista,
        BigDecimal temperatura,
        BigDecimal sensacaoTermica,
        BigDecimal chuvaMm,
        Integer probabilidadeChuva,
        BigDecimal ventoVelocidade,
        Integer codigoClima,
        String descricaoClima
) {}
