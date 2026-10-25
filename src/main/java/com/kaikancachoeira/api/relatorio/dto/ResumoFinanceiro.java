package com.kaikancachoeira.api.relatorio.dto;

import java.math.BigDecimal;

public record ResumoFinanceiro(
        BigDecimal receitaVendas,
        BigDecimal descontosVendas,
        BigDecimal custoCompras,
        BigDecimal custoFichasAgradecimento,
        BigDecimal custoIngredientes,
        BigDecimal totalCustosAdicionais,
        BigDecimal doacoesEmDinheiro,
        BigDecimal receitaTotal,
        BigDecimal custoTotal,
        BigDecimal lucro,
        long vendas
) {
}
