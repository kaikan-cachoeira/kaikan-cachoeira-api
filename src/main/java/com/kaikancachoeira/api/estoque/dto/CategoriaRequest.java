package com.kaikancachoeira.api.estoque.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @Schema(description = "Nome da categoria, único sem diferenciar maiúsculas. Espaços externos são removidos", example = "BEBIDA")
        @NotBlank @Size(max = 100)
        String nome,

        @Schema(description = "Descrição opcional", example = "Todos os produtos classificados como bebida")
        @Size(max = 255)
        String descricao
) {}
