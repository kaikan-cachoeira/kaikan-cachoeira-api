package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record TrocaEmailRequest(
        @NotBlank
        @Email
        String email
) {
}
