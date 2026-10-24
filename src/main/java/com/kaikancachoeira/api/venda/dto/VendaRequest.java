package com.kaikancachoeira.api.venda.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record VendaRequest(
        @PositiveOrZero(message = "Valor final deve ser maior ou igual a 0")
        @Digits(integer = 8, fraction = 2, message = "Valor final inválido")
        BigDecimal valorFinal,

        @NotEmpty(message = "Informe ao menos um item")
        List<@Valid ItemVendaRequest> itens
) {}
