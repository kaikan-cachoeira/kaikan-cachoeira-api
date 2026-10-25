package com.kaikancachoeira.api.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record FotoResponse(
        @Schema(description = "URL relativa da foto salva", example = "/arquivos/3f2a....png") String url
) {}
