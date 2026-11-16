package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class PagamentoConstraintsTest extends DbIntegrationTest {

    @Test
    void valorDeveSerMaiorQueZero() {
        Long config = insertConfiguracaoAnuidade(2026, bd("150.00"));
        Long associado = insertAssociado("Associado", java.time.LocalDate.of(1982, 6, 10), "TITULAR", null, "11922222222");
        Long anuidade = insertAnuidade(associado, config, bd("150.00"), "PENDENTE");

        assertViolacao(() -> insertPagamentoAnuidade(anuidade, bd("0.00")), "chk_valor");
    }

    @Test
    void pagamentoPrecisaDeAnuidadeExistente() {
        assertViolacao(() -> insertPagamentoAnuidade(999999L, bd("10.00")), "fk_anuidade_pagamento");
    }

    @Test
    void pagamentoPodeSerDeletadoSemAfetarOutrosDaMesmaAnuidade() {
        Long config = insertConfiguracaoAnuidade(2026, bd("150.00"));
        Long associado = insertAssociado("Associado", java.time.LocalDate.of(1982, 6, 10), "TITULAR", null, "11922222222");
        Long anuidade = insertAnuidade(associado, config, bd("150.00"), "PAGO");
        Long pagamento1 = insertPagamentoAnuidade(anuidade, bd("50.00"));
        Long pagamento2 = insertPagamentoAnuidade(anuidade, bd("100.00"));

        jdbc.update("delete from pagamento where id = ?", pagamento1);

        assertThat(count("select count(*) from pagamento where id = ?", pagamento1)).isZero();
        assertThat(count("select count(*) from pagamento where id = ?", pagamento2)).isEqualTo(1);
        assertThat(count("select count(*) from anuidade where id = ?", anuidade)).isEqualTo(1);
    }
}
