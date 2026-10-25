package com.kaikancachoeira.api.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record RefreshTokenResponse(
        @Schema(description = "Novo access token") String accessToken,
        @Schema(description = "Novo refresh token. O anterior é revogado (rotação)") String refreshToken
) {}
