package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "Ranking agregado dos produtos mais vendidos.")
public record ProdutosMaisVendidosResponse(List<Item> items) {

    @Schema(description = "Produto e valores agregados das vendas.")
    public record Item(
            String name,
            long sold,
            BigDecimal revenue
    ) {}
}
