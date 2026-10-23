package com.kaikancachoeira.api.estoque.integration;

import com.kaikancachoeira.api.estoque.service.MovimentacaoService;
import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.AssertionsForClassTypes.assertThatCode;

class MovimentacaoHistoricoIntegrationTest extends DbIntegrationTest {

    @Autowired
    private MovimentacaoService service;

    @Test
    void historicoSemFiltrosNaoDeveFalhar() {
        Long produto = insertProduto("Coxinha");

        assertThatCode(() -> service.historicoDoProduto(produto, null, null, null, null, 0, 20))
                .doesNotThrowAnyException();
    }
}
