package com.kaikancachoeira.api.clima.client;

import java.time.Instant;
import java.util.List;

public interface ClimaClient {

    List<ClimaPrevisaoHoraria> consultar(Instant inicio, Instant fim);
}
