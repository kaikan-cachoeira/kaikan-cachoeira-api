package com.kaikancachoeira.api.evento.dto;

import java.math.BigDecimal;

public record ResumoEvento(
        Long quantidadeProdutos,
        Long quantidadeVendas,
        BigDecimal totalVendido,
        BigDecimal totalCustosAdicionais
) {}
