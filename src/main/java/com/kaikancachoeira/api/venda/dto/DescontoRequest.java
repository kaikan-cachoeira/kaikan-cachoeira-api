package com.kaikancachoeira.api.venda.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record DescontoRequest(
        @NotNull(message = "Novo preço é obrigatório")
        @PositiveOrZero(message = "Novo preço deve ser maior ou igual a 0")
        @Digits(integer = 8, fraction = 2, message = "Novo preço inválido")
        BigDecimal novoPreco
) {}
