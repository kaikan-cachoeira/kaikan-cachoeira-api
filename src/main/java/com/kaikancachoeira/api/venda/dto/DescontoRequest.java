package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DescontoRequest(
        @Schema(description = "Novo preço unitário, entre 0 e o preço de venda do produto", example = "7.00")
        @NotNull(message = "Novo preço é obrigatório")
        @PositiveOrZero(message = "Novo preço deve ser maior ou igual a 0")
        @Digits(integer = 8, fraction = 2, message = "Novo preço inválido")
        BigDecimal novoPreco
) {}
