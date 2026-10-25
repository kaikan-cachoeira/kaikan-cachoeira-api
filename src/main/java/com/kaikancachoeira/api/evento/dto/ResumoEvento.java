package com.kaikancachoeira.api.evento.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ResumoEvento(
        @Schema(description = "Itens no cardápio") Long quantidadeProdutos,
        @Schema(description = "Vendas não canceladas") Long quantidadeVendas,
        @Schema(description = "Soma do preço total das vendas não canceladas") BigDecimal totalVendido,
        @Schema(description = "Soma dos custos adicionais do evento") BigDecimal totalCustosAdicionais
) {}
