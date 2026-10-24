package com.kaikancachoeira.api.relatorio.dto;

import java.math.BigDecimal;
import java.util.List;

public record DoacoesRelatorio(
        List<DoacaoItemRelatorio> itens,
        List<DoacaoDinheiroRelatorio> emDinheiro,
        BigDecimal totalEmDinheiro
) {
}
