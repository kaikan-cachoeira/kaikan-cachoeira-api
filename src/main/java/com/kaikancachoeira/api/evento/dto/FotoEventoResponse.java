package com.kaikancachoeira.api.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FotoEventoResponse(
        @Schema(description = "URL relativa da foto salva", example = "/arquivos/3f2a....png") String url
) {}
