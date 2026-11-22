package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class AnuidadeConstraintsTest extends DbIntegrationTest {

    @Test
    void statusDeveAceitarSomenteOsValoresEsperados() {
        Long config = insertConfiguracaoAnuidade(2026, bd("150.00"));
        Long associado = insertAssociado("Associado", java.time.LocalDate.of(1982, 6, 10), "TITULAR", null, "11922222222");

        assertViolacao(() -> insertAnuidade(associado, config, bd("150.00"), "ATRASADO"),
                "chk_status");
    }

    @Test
    void anuidadePodeReceberMaisDeUmPagamento() {
        Long config = insertConfiguracaoAnuidade(2026, bd("150.00"));
        Long associado = insertAssociado("Associado", java.time.LocalDate.of(1982, 6, 10), "TITULAR", null, "11922222222");
        Long anuidade = insertAnuidade(associado, config, bd("150.00"), "PENDENTE");

        assertThatCode(() -> {
            insertPagamentoAnuidade(anuidade, bd("50.00"));
            insertPagamentoAnuidade(anuidade, bd("100.00"));
        }).doesNotThrowAnyException();

        assertThat(count("select count(*) from pagamento where anuidade_id = ?", anuidade)).isEqualTo(2);
    }

    @Test
    void excluirConfiguracaoDeveRemoverAnuidadesEPagamentosVinculados() {
        Long config = insertConfiguracaoAnuidade(2026, bd("150.00"));
        Long associado = insertAssociado("Associado", java.time.LocalDate.of(1982, 6, 10), "TITULAR", null, "11922222222");
        Long anuidade = insertAnuidade(associado, config, bd("150.00"), "PAGO");
        Long pagamento = insertPagamentoAnuidade(anuidade, bd("150.00"));

        jdbc.update("delete from configuracao_anuidade where id = ?", config);

        assertThat(count("select count(*) from anuidade where id = ?", anuidade)).isZero();
        assertThat(count("select count(*) from pagamento where id = ?", pagamento)).isZero();
    }
}
