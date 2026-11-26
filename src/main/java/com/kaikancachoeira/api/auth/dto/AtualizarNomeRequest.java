package com.kaikancachoeira.api.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtualizarNomeRequest(@NotBlank @Size(min = 1, max = 150) String nome) {
}
