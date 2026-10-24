package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CaixaRequest(
        @NotNull(message = "Tipo é obrigatório")
        TipoLancamento tipo,

        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que 0")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal valor,

        @NotNull(message = "Forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,

        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeDevedor,

        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        String observacao
) {}
