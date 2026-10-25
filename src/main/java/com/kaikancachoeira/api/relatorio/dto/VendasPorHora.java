package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record VendasPorHora(
        @Schema(description = "Total de vendas não canceladas") long quantidadeVendasTotais,
        @Schema(description = "Quantidade de vendas por hora, em ordem crescente. Fuso America/Sao_Paulo") List<VendaHora> ranking
) {}
