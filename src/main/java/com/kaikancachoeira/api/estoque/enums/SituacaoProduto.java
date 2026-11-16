package com.kaikancachoeira.api.estoque.enums;

import com.kaikancachoeira.api.common.exception.CampoErro;
import com.kaikancachoeira.api.common.exception.RequisicaoInvalidaException;

import java.util.List;
import java.util.Locale;

public enum SituacaoProduto {
    OK("ok"),
    OUT("out"),
    LOW("low");

    private final String codigo;

    SituacaoProduto(String codigo) {
        this.codigo = codigo;
    }

    public String codigo() {
        return codigo;
    }

    public static SituacaoProduto fromCodigo(String valor) {
        if (valor == null) {
            return null;
        }

        if (valor.isBlank()) {
            throw new RequisicaoInvalidaException(
                    "REQUISICAO_INVALIDA",
                    "Valor inválido para o parâmetro",
                    List.of(new CampoErro("situacao", "Valor inválido"))
            );
        }

        String normalizado = valor.trim().toLowerCase(Locale.ROOT);
        for (SituacaoProduto situacao : values()) {
            if (situacao.codigo.equals(normalizado)) {
                return situacao;
            }
        }

        throw new RequisicaoInvalidaException(
                "REQUISICAO_INVALIDA",
                "Valor inválido para o parâmetro",
                List.of(new CampoErro("situacao", "Valor inválido"))
        );
    }
}
