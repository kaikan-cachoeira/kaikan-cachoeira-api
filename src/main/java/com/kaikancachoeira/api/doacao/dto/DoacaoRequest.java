package com.kaikancachoeira.api.doacao.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DoacaoRequest(
        @NotNull(message = "Informe o evento") Long eventoId,
        @NotBlank(message = "Informe o tipo da doação") String tipo,
        @Digits(integer = 8, fraction = 2, message = "Valor inválido") BigDecimal valor,
        String formaPagamento,
        Long produtoId,
        @Size(max = 150, message = "Máximo de 150 caracteres") String nome,
        Integer quantidade,
        @NotBlank(message = "Informe o doador")
        @Size(max = 150, message = "Máximo de 150 caracteres") String doador,
        @PositiveOrZero(message = "O valor da ficha deve ser maior ou igual a zero")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido") BigDecimal valorFicha
) {}
