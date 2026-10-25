package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Relatório do evento. Para EVENTO retorna RelatorioEventoResponse; para REUNIAO, RelatorioReuniaoResponse",
        oneOf = {RelatorioEventoResponse.class, RelatorioReuniaoResponse.class})
public sealed interface RelatorioResponse permits RelatorioEventoResponse, RelatorioReuniaoResponse {}