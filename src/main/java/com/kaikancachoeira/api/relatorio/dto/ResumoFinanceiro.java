package com.kaikancachoeira.api.relatorio.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

public record ResumoFinanceiro(
        @Schema(description = "Soma do preço total das vendas não canceladas (já com descontos)") BigDecimal receitaVendas,
        @Schema(description = "Soma dos descontos dados na venda inteira. Informativo") BigDecimal descontosVendas,
        @Schema(description = "Compras (REABASTECIMENTO) do evento") BigDecimal custoCompras,
        @Schema(description = "Soma dos valores de ficha de agradecimento") BigDecimal custoFichasAgradecimento,
        @Schema(description = "Soma do custo dos ingredientes dos itens PRODUZIDO") BigDecimal custoIngredientes,
        BigDecimal totalCustosAdicionais,
        BigDecimal doacoesEmDinheiro,
        @Schema(description = "receitaVendas + doacoesEmDinheiro") BigDecimal receitaTotal,
        @Schema(description = "custoCompras + custoFichasAgradecimento + custoIngredientes + totalCustosAdicionais") BigDecimal custoTotal,
        @Schema(description = "receitaTotal - custoTotal. Pode ser negativo") BigDecimal lucro,
        @Schema(description = "Quantidade de vendas não canceladas") long vendas
) {}
