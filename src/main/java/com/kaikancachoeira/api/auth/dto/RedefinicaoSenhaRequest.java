package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedefinicaoSenhaRequest(
        @Schema(description = "Token recebido por e-mail (64 caracteres hexadecimais)")
        @NotBlank
        String token,

        @Schema(description = "Nova senha, de 8 a 255 caracteres", example = "NovaSenha@987")
        @NotBlank
        @Size(min = 8, max = 255)
        String novaSenha
) {}
