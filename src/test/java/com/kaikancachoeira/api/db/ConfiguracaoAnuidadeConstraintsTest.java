package com.kaikancachoeira.api.db;

import com.kaikancachoeira.api.support.DbIntegrationTest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class ConfiguracaoAnuidadeConstraintsTest extends DbIntegrationTest {

    @Test
    void anoDeveSerUnico() {
        insertConfiguracaoAnuidade(2026, bd("120.00"));

        assertViolacao(() -> insertConfiguracaoAnuidade(2026, bd("150.00")),
                "configuracao_anuidade_ano_key");
    }

    @Test
    void valorDeveSerPersistido() {
        Long id = insertConfiguracaoAnuidade(2027, bd("180.50"));

        assertThat(jdbc.queryForObject("select valor from configuracao_anuidade where id = ?", BigDecimal.class, id))
                .isEqualByComparingTo("180.50");
    }

    @Test
    void excluirConfiguracaoDeveRemoverAnuidadesVinculadas() {
        Long config = insertConfiguracaoAnuidade(2028, bd("200.00"));
        Long titular = insertAssociado("Titular", java.time.LocalDate.of(1980, 1, 1), "TITULAR", null, "11911111111");
        Long anuidade = insertAnuidade(titular, config, bd("200.00"), "PENDENTE");

        jdbc.update("delete from configuracao_anuidade where id = ?", config);

        assertThat(count("select count(*) from configuracao_anuidade where id = ?", config)).isZero();
        assertThat(count("select count(*) from anuidade where id = ?", anuidade)).isZero();
    }
}
