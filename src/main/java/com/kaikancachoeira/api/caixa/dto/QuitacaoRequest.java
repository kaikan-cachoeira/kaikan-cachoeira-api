package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record QuitacaoRequest(
        @Schema(description = "Forma usada na quitação. FIADO não é aceito (400)",
                allowableValues = {"PIX", "DEBITO", "CREDITO", "DINHEIRO"}, example = "DINHEIRO")
        @NotNull(message = "Forma de pagamento é obrigatória")
        FormaPagamento formaPagamento
) {}
