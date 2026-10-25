package com.kaikancachoeira.api.venda.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;
import java.util.List;

public record VendaRequest(
        @Schema(description = "Valor final cobrado (opcional), entre 0 e o total dos itens já com o desconto unitário. "
                + "O desconto da venda é total - valorFinal", example = "25.00")
        @PositiveOrZero(message = "Valor final deve ser maior ou igual a 0")
        @Digits(integer = 8, fraction = 2, message = "Valor final inválido")
        BigDecimal valorFinal,

        @Schema(description = "Itens da venda, ao menos um, sem produto repetido")
        @NotEmpty(message = "Informe ao menos um item")
        List<@Valid ItemVendaRequest> itens
) {}