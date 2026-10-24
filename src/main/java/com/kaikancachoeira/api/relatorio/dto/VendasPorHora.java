package com.kaikancachoeira.api.relatorio.dto;

import java.util.List;

public record VendasPorHora(
        long quantidadeVendasTotais,
        List<VendaHora> ranking
) {
}
