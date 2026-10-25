package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RecuperacaoSenhaRequest(
        @Schema(description = "E-mail da conta. A resposta é sempre 202, exista o e-mail ou não", example = "admin@email.com")
        @NotBlank
        @Email
        @Size(max = 255)
        String email
) {}
