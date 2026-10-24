package com.kaikancachoeira.api.doacao.enums;

import java.util.Arrays;
import java.util.Optional;

public enum FormaPagamentoDoacao {
    PIX,
    DEBITO,
    CREDITO,
    DINHEIRO;

    public static Optional<FormaPagamentoDoacao> de(String valor) {
        if (valor == null) {
            return Optional.empty();
        }
        String normalizado = valor.trim();
        return Arrays.stream(values())
                .filter(f -> f.name().equalsIgnoreCase(normalizado))
                .findFirst();
    }
}
