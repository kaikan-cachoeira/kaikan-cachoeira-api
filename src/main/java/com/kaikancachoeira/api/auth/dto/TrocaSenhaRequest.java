package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrocaSenhaRequest(
        @NotBlank
        String senhaAtual,

        @NotBlank
        @Size(min = 8, max = 255)
        String novaSenha
) {
}