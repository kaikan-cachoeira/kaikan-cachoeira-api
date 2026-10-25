package com.kaikancachoeira.api.caixa.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import com.kaikancachoeira.api.caixa.entity.Fiado;
import com.kaikancachoeira.api.caixa.entity.FormaPagamento;

import java.math.BigDecimal;
import java.time.Instant;

@Schema(description = "Resposta com os dados de um fiado registrado no caixa.")
public record FiadoResponse(
        @Schema(description = "Identificador do fiado.")
        Long id,
        @Schema(description = "Nome da pessoa responsável pelo pagamento pendente.")
        String nomeDevedor,
        @Schema(description = "Valor do fiado em reais.")
        BigDecimal valor,
        @Schema(description = "Observações registradas para o fiado; pode ser nulo.")
        String observacoes,
        @Schema(description = "Indica se o fiado foi quitado.")
        boolean quitado,
        @Schema(description = "Data e hora da quitação em ISO-8601; nulo enquanto pendente.")
        Instant quitadoEm,
        @Schema(description = "Forma de pagamento utilizada na quitação; nula enquanto pendente.")
        FormaPagamento formaPagamento,
        @Schema(description = "Data e hora de criação do lançamento em ISO-8601.")
        Instant criadoEm
) {
    public static FiadoResponse de(Fiado fiado) {
        return new FiadoResponse(fiado.getId(), fiado.getNomeDevedor(), fiado.getValor(), fiado.getObservacoes(),
                fiado.getQuitado(), fiado.getQuitadoEm(), fiado.getFormaPagamento(), fiado.getCriadoEm());
    }
}