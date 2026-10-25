package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record LogoutRequest(
        @Schema(description = "Refresh token a ser revogado")
        @NotBlank
        String refreshToken
) {}