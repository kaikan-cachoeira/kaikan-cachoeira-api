package com.kaikancachoeira.api.doacao.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record DoacaoRequest(
        @Schema(description = "Evento da doação. Deve existir e não estar ENCERRADO", example = "1")
        @NotNull(message = "Informe o evento")
        Long eventoId,

        @Schema(description = "Tipo, sem diferenciar maiúsculas. Imutável na edição",
                allowableValues = {"dinheiro", "produto", "reabastecimento"}, example = "dinheiro")
        @NotBlank(message = "Informe o tipo da doação")
        String tipo,

        @Schema(description = "Só em dinheiro: valor maior que 0", example = "50.00")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal valor,

        @Schema(description = "Só em dinheiro: PIX, DEBITO, CREDITO ou DINHEIRO", example = "PIX")
        String formaPagamento,

        @Schema(description = "Só em reabastecimento: produto ativo do estoque. Imutável na edição", example = "3")
        Long produtoId,

        @Schema(description = "Só em produto: descrição do item. Em reabastecimento é preenchido pelo nome do produto",
                example = "Bolo de cenoura")
        @Size(max = 150, message = "Máximo de 150 caracteres")
        String nome,

        @Schema(description = "Só em reabastecimento: quantidade maior que 0. Cria lote sem custo", example = "12")
        Integer quantidade,

        @Schema(description = "Nome do doador (texto livre)", example = "Maria Silva")
        @NotBlank(message = "Informe o doador")
        @Size(max = 150, message = "Máximo de 150 caracteres")
        String doador,

        @Schema(description = "Valor da ficha de agradecimento (opcional). Entra como custo do evento", example = "20.00")
        @PositiveOrZero(message = "O valor da ficha deve ser maior ou igual a zero")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal valorFicha
) {}