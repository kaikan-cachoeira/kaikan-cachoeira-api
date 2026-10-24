package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import jakarta.validation.constraints.NotNull;

public record QuitacaoRequest(
        @NotNull(message = "Forma de pagamento é obrigatória")
        FormaPagamento formaPagamento
) {}
