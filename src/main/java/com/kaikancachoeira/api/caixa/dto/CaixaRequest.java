package com.kaikancachoeira.api.caixa.dto;

import com.kaikancachoeira.api.caixa.entity.FormaPagamento;
import com.kaikancachoeira.api.caixa.entity.TipoLancamento;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CaixaRequest(
        @Schema(description = "Tipo do lançamento. QUITACAO_FIADO não é aceito neste endpoint",
                allowableValues = {"VENDA_FICHA", "DEVOLUCAO_FICHA"}, example = "VENDA_FICHA")
        @NotNull(message = "Tipo é obrigatório")
        TipoLancamento tipo,

        @Schema(description = "Valor das fichas, maior que 0, com até 2 casas decimais", example = "50.00")
        @NotNull(message = "Valor é obrigatório")
        @Positive(message = "Valor deve ser maior que 0")
        @Digits(integer = 8, fraction = 2, message = "Valor inválido")
        BigDecimal valor,

        @Schema(description = "Forma de pagamento. FIADO só é aceito em VENDA_FICHA", example = "PIX")
        @NotNull(message = "Forma de pagamento é obrigatória")
        FormaPagamento formaPagamento,

        @Schema(description = "Nome do devedor. Obrigatório com FIADO; ignorado nas demais formas", example = "Maria")
        @Size(max = 150, message = "Nome deve ter no máximo 150 caracteres")
        String nomeDevedor,

        @Schema(description = "Observação do fiado (opcional)", example = "paga no próximo evento")
        @Size(max = 255, message = "Observação deve ter no máximo 255 caracteres")
        String observacao
) {}