package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.enums.OrigemProdutoEvento;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record ProdutoEventoRequest(

        @NotNull
        OrigemProdutoEvento origem,

        Long produtoId,

        @Size(max = 150)
        String nome,

        String descricao,

        @Size(max = 100)
        String categoria,

        @NotNull
        @PositiveOrZero
        BigDecimal precoVenda,

        @Positive
        Integer quantidadePrevista,

        @PositiveOrZero
        BigDecimal custoTotalIngredientes
) {}