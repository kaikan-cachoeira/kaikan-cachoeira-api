package com.kaikancachoeira.api.auth.dto;

public record RefreshTokenResponse(
        String accessToken,
        String refreshToken
) {}
