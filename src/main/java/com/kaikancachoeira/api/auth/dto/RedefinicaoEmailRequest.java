package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record RedefinicaoEmailRequest(
        @NotBlank
        String token
) {
}