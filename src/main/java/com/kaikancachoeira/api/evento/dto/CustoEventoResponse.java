package com.kaikancachoeira.api.evento.dto;

import com.kaikancachoeira.api.evento.entity.CustoEvento;

import java.math.BigDecimal;
import java.time.Instant;

public record CustoEventoResponse(
        Long id,
        String nome,
        String descricao,
        BigDecimal valor,
        Instant criadoEm,
        Instant atualizadoEm
) {

    public static CustoEventoResponse de(CustoEvento custo) {
        return new CustoEventoResponse(
                custo.getId(),
                custo.getNome(),
                custo.getDescricao(),
                custo.getValor(),
                custo.getCriadoEm(),
                custo.getAtualizadoEm()
        );
    }
}
