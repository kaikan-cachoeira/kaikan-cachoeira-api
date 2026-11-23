package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TrocaSenhaRequest(
        @NotNull
        Integer idUsuario,

        @NotBlank
        @Size(min = 8, max = 255)
        String senha
) {
}