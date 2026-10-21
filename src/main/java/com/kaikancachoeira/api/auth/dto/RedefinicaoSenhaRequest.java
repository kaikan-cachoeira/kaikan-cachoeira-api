package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedefinicaoSenhaRequest(
        @NotBlank
        String token,

        @NotBlank
        @Size(min = 8, max = 255)
        String novaSenha
) {}
