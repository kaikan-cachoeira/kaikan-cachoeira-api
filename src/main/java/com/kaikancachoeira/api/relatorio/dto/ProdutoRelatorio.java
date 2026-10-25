package com.kaikancachoeira.api.relatorio.dto;

import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ProdutoRelatorio(
        Long produtoEventoId,
        String nome,
        OrigemProdutoEvento origem,
        @Schema(description = "Nulo em itens PRODUZIDO", nullable = true) Integer quantidadePrevista,
        long quantidadeVendida,
        @Schema(description = "Soma de quantidade x descontoUnitario") BigDecimal totalDescontos,
        @Schema(description = "ESTOQUE: custo dos lotes consumidos nas vendas (lote sem custo conta 0). PRODUZIDO: custo dos ingredientes")
        BigDecimal custoTotal,
        @Schema(description = "Soma de quantidade x (precoUnitario - descontoUnitario)") BigDecimal receitaVendas,
        @Schema(description = "Total arrecadado do produto, sem descontar custos") BigDecimal lucroBruto,
        @Schema(description = "receitaVendas - custoTotal") BigDecimal lucroLiquido,
        @Schema(description = "Preço de venda, sem descontos") BigDecimal precoVendido
) {}
