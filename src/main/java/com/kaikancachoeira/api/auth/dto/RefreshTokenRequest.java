package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @Schema(description = "Refresh token recebido no login ou na última renovação")
        @NotBlank
        String refreshToken
) {}
