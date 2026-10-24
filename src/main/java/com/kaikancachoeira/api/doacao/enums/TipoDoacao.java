package com.kaikancachoeira.api.doacao.enums;

import java.util.Arrays;
import java.util.Optional;

public enum TipoDoacao {
    DINHEIRO,
    PRODUTO,
    REABASTECIMENTO;

    public static Optional<TipoDoacao> de(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(values())
                .filter(t -> t.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
